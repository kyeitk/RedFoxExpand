package redfoxexpand.client.resource;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import redfoxexpand.core.DefinitionCandidate;
import redfoxexpand.core.DefinitionRegistry;
import redfoxexpand.core.GuiDefinition;
import redfoxexpand.core.ResourceLimits;
import redfoxexpand.core.SchemaV2Parser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/** Native ResourceManager discovery, strict parsing and texture validation. */
public final class SnapshotLoader {
    public static final Identifier INDEX = Identifier.fromNamespaceAndPath("kyeitk", "redfoxexpand/index.json");
    private static final AtomicLong NEXT_GENERATION = new AtomicLong();
    private static final Set<String> MANIFEST_FIELDS = Set.of("api_version", "configs");

    private final SchemaV2Parser schema = new SchemaV2Parser();

    public ResourceSnapshot load(ResourceManager manager) {
        MutableReport report = new MutableReport();
        List<Resource> manifests = new ArrayList<>(manager.getResourceStack(INDEX));
        Collections.reverse(manifests); // Native stack is high -> low; candidates apply low -> high.
        report.manifests = manifests.size();
        List<DefinitionCandidate> candidates = new ArrayList<>();
        Map<GuiDefinition.TextureSpec, Identifier> textures = new LinkedHashMap<>();
        Map<Identifier, Long> validated = new HashMap<>();
        long[] reloadPixels = {0L};

        for (int sourcePriority = 0; sourcePriority < manifests.size(); sourcePriority++) {
            Resource manifest = manifests.get(sourcePriority);
            String pack = manifest.sourcePackId();
            try {
                for (Identifier config : parseManifest(manifest, pack, report)) {
                    report.configs++;
                    Optional<Resource> resource = resourceFromPack(manager.getResourceStack(config), pack);
                    if (resource.isEmpty()) {
                        report.error(pack + " manifest references missing same-pack config " + config);
                        continue;
                    }
                    loadConfig(manager, resource.get(), config, pack, sourcePriority,
                            candidates, textures, validated, reloadPixels, report);
                }
            } catch (Exception error) {
                report.error("Invalid manifest from " + pack + ": " + rootMessage(error));
            }
        }

        List<DefinitionCandidate> active = DefinitionRegistry.resolve(candidates);
        report.candidates = candidates.size();
        report.activeDefinitions = active.size();
        report.validatedTextures = validated.size();
        return new ResourceSnapshot(NEXT_GENERATION.incrementAndGet(), active, textures, report.freeze());
    }

    private List<Identifier> parseManifest(Resource resource, String pack, MutableReport report) throws IOException {
        byte[] bytes = readLimited(resource.open(), ResourceLimits.MAX_JSON_BYTES, INDEX.toString());
        JsonElement parsed = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
        if (!parsed.isJsonObject()) throw new IllegalArgumentException("manifest root must be an object");
        JsonObject json = parsed.getAsJsonObject();
        Set<String> unknown = new HashSet<>(json.keySet());
        unknown.removeAll(MANIFEST_FIELDS);
        if (!unknown.isEmpty()) throw new IllegalArgumentException("unknown manifest field(s): " + unknown);
        if (!json.has("api_version") || json.get("api_version").getAsInt() != 2) {
            throw new IllegalArgumentException("manifest api_version must be 2");
        }
        JsonArray configs = json.getAsJsonArray("configs");
        if (configs == null || configs.size() > ResourceLimits.MAX_MANIFEST_CONFIGS) {
            throw new IllegalArgumentException("manifest configs must contain at most " + ResourceLimits.MAX_MANIFEST_CONFIGS + " entries");
        }
        LinkedHashSet<Identifier> result = new LinkedHashSet<>();
        for (JsonElement element : configs) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("manifest config entries must be strings");
            }
            String configured = ResourceLimits.safePath(element.getAsString(), "manifest config");
            Identifier id = configured.contains(":")
                    ? Identifier.parse(configured)
                    : Identifier.fromNamespaceAndPath("kyeitk", configured);
            if (!id.getNamespace().equals("kyeitk")
                    || !id.getPath().startsWith("redfoxexpand/config/")
                    || !id.getPath().endsWith(".json")) {
                throw new IllegalArgumentException("config must be under kyeitk:redfoxexpand/config/: " + id);
            }
            if (!result.add(id)) report.warning(pack + " manifest repeats config " + id);
        }
        if (result.size() > ResourceLimits.MAX_CONFIGS_PER_PACK) {
            throw new IllegalArgumentException("pack config count exceeds " + ResourceLimits.MAX_CONFIGS_PER_PACK);
        }
        return List.copyOf(result);
    }

    private void loadConfig(ResourceManager manager, Resource resource, Identifier location,
                            String pack, int sourcePriority, List<DefinitionCandidate> candidates,
                            Map<GuiDefinition.TextureSpec, Identifier> textures,
                            Map<Identifier, Long> validated, long[] reloadPixels,
                            MutableReport report) {
        try (InputStream stream = resource.open();
             InputStreamReader reader = new InputStreamReader(
                     new ByteArrayInputStream(readLimited(stream, ResourceLimits.MAX_JSON_BYTES, location.toString())),
                     StandardCharsets.UTF_8)) {
            List<SchemaV2Parser.ParsedDefinition> parsed = schema.parse(reader, location + " from " + pack);
            for (int index = 0; index < parsed.size(); index++) {
                SchemaV2Parser.ParsedDefinition definition = parsed.get(index);
                try {
                    validateDefinition(manager, definition.definition(), textures, validated, reloadPixels, report);
                    candidates.add(new DefinitionCandidate(definition.id(), 2, pack, sourcePriority,
                            location.toString(), index, definition.matcher(), definition.operation(),
                            definition.priority(), definition.definition()));
                } catch (Exception error) {
                    report.error(location + " definition " + definition.id() + " rejected: " + rootMessage(error));
                }
            }
        } catch (Exception error) {
            report.error("Invalid config " + location + " from " + pack + ": " + rootMessage(error));
        }
    }

    private void validateDefinition(ResourceManager manager, GuiDefinition definition,
                                    Map<GuiDefinition.TextureSpec, Identifier> textures,
                                    Map<Identifier, Long> validated, long[] reloadPixels,
                                    MutableReport report) throws IOException {
        for (GuiDefinition.Sprite sprite : definition.sprites()) {
            if (sprite.texture().type() == GuiDefinition.ResourceType.GUI_SPRITE && !sprite.fullTexture()) {
                throw new IllegalArgumentException("gui_sprite only supports full_texture rendering");
            }
            Identifier base = validateTexture(manager, sprite.texture(), textures, validated, reloadPixels);
            GuiDefinition.Animation animation = sprite.animation();
            if (animation == null) continue;
            validateTexture(manager, animation.defaultTexture(), textures, validated, reloadPixels);
            long animationPixels = 0L;
            boolean missing = false;
            for (GuiDefinition.AnimationFrame frame : animation.frames()) {
                try {
                    Identifier id = validateTexture(manager, frame.texture(), textures, validated, reloadPixels);
                    animationPixels += validated.getOrDefault(rawTextureLocation(frame.texture(), id), 0L);
                } catch (Exception error) {
                    missing = true;
                    report.warning("Animation frame " + frame.texture().location() + " unavailable: " + rootMessage(error));
                    if (animation.missingFrameBehavior() == GuiDefinition.MissingFrameBehavior.DISABLE) break;
                }
                if (animationPixels > ResourceLimits.MAX_ANIMATION_PIXELS) {
                    throw new IllegalArgumentException("animation pixel budget exceeds " + ResourceLimits.MAX_ANIMATION_PIXELS);
                }
            }
            if (missing && animation.missingFrameBehavior() == GuiDefinition.MissingFrameBehavior.DISABLE) {
                report.warning("Animation disabled; default texture will be used: " + base);
            }
        }
    }

    private Identifier validateTexture(ResourceManager manager, GuiDefinition.TextureSpec spec,
                                       Map<GuiDefinition.TextureSpec, Identifier> textures,
                                       Map<Identifier, Long> validated, long[] reloadPixels) throws IOException {
        Identifier renderId = resolve(spec);
        Identifier rawId = rawTextureLocation(spec, renderId);
        Long knownPixels = validated.get(rawId);
        if (knownPixels == null) {
            Resource resource = manager.getResource(rawId)
                    .orElseThrow(() -> new IllegalArgumentException("missing texture resource " + rawId));
            byte[] png = readLimited(resource.open(), ResourceLimits.MAX_PNG_BYTES, rawId.toString());
            ImageDimensions header = pngHeaderDimensions(png, rawId.toString());
            long headerPixels = checkImageDimensions(header.width(), header.height(), rawId.toString());
            try (NativeImage image = NativeImage.read(png)) {
                long decodedPixels = checkImageDimensions(image.getWidth(), image.getHeight(), rawId.toString());
                if (image.getWidth() != header.width() || image.getHeight() != header.height()) {
                    throw new IllegalArgumentException("PNG header and decoded dimensions disagree for " + rawId);
                }
                knownPixels = decodedPixels;
            }
            reloadPixels[0] += knownPixels;
            if (reloadPixels[0] > ResourceLimits.MAX_RELOAD_PIXELS) {
                throw new IllegalArgumentException("reload texture budget exceeds " + ResourceLimits.MAX_RELOAD_PIXELS + " pixels");
            }
            validated.put(rawId, knownPixels);
        }
        textures.put(spec, renderId);
        return renderId;
    }

    static Identifier resolve(GuiDefinition.TextureSpec spec) {
        String configured = spec.location();
        if (spec.type() == GuiDefinition.ResourceType.PACK_RESOURCE) {
            if (configured.contains(":")) return Identifier.parse(configured);
            String path = configured.startsWith("redfoxexpand/")
                    ? configured : "redfoxexpand/" + configured;
            return Identifier.fromNamespaceAndPath("kyeitk", path);
        }
        return Identifier.parse(configured);
    }

    static Identifier rawTextureLocation(GuiDefinition.TextureSpec spec, Identifier renderId) {
        if (spec.type() != GuiDefinition.ResourceType.GUI_SPRITE) return renderId;
        return Identifier.fromNamespaceAndPath(renderId.getNamespace(),
                "textures/gui/sprites/" + renderId.getPath() + ".png");
    }

    private static Optional<Resource> resourceFromPack(List<Resource> stack, String pack) {
        return stack.stream().filter(resource -> resource.sourcePackId().equals(pack)).findFirst();
    }

    private static byte[] readLimited(InputStream input, int maximum, String label) throws IOException {
        try (input) {
            byte[] result = input.readNBytes(maximum + 1);
            if (result.length > maximum) throw new IOException(label + " exceeds " + maximum + " bytes");
            return result;
        }
    }

    private static ImageDimensions pngHeaderDimensions(byte[] png, String label) {
        byte[] signature = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        if (png.length < 24) throw new IllegalArgumentException(label + " is not a complete PNG");
        for (int i = 0; i < signature.length; i++) {
            if (png[i] != signature[i]) throw new IllegalArgumentException(label + " has an invalid PNG signature");
        }
        if (png[12] != 'I' || png[13] != 'H' || png[14] != 'D' || png[15] != 'R') {
            throw new IllegalArgumentException(label + " has no leading IHDR chunk");
        }
        ByteBuffer buffer = ByteBuffer.wrap(png, 16, 8).order(ByteOrder.BIG_ENDIAN);
        long width = Integer.toUnsignedLong(buffer.getInt());
        long height = Integer.toUnsignedLong(buffer.getInt());
        if (width > Integer.MAX_VALUE || height > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(label + " has dimensions outside the supported integer range");
        }
        return new ImageDimensions((int) width, (int) height);
    }

    static long checkImageDimensions(int width, int height, String label) {
        if (width <= 0 || height <= 0
                || width > ResourceLimits.MAX_IMAGE_DIMENSION
                || height > ResourceLimits.MAX_IMAGE_DIMENSION) {
            throw new IllegalArgumentException(label + " exceeds the "
                    + ResourceLimits.MAX_IMAGE_DIMENSION + " px per-side limit");
        }
        long pixels = Math.multiplyExact((long) width, height);
        if (pixels > ResourceLimits.MAX_IMAGE_PIXELS) {
            throw new IllegalArgumentException(label + " exceeds " + ResourceLimits.MAX_IMAGE_PIXELS + " pixels");
        }
        return pixels;
    }

    private record ImageDimensions(int width, int height) {
    }

    private static String rootMessage(Throwable error) {
        Throwable cursor = error;
        while (cursor.getCause() != null) cursor = cursor.getCause();
        return cursor.getMessage() == null ? cursor.getClass().getSimpleName() : cursor.getMessage();
    }

    private static final class MutableReport {
        int manifests;
        int configs;
        int candidates;
        int activeDefinitions;
        int validatedTextures;
        int warnings;
        int errors;
        final List<String> messages = new ArrayList<>();

        void warning(String message) { warnings++; messages.add("WARN " + message); }
        void error(String message) { errors++; messages.add("ERROR " + message); }
        ResourceSnapshot.LoadReport freeze() {
            return new ResourceSnapshot.LoadReport(manifests, configs, candidates, activeDefinitions,
                    validatedTextures, warnings, errors, messages);
        }
    }
}
