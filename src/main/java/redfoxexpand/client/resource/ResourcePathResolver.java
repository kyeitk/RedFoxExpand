package redfoxexpand.client.resource;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import redfoxexpand.client.compat.LegacyResourceAdapter;
import redfoxexpand.client.config.AnimationDefinition;
import redfoxexpand.client.config.GuiTextureResolver;
import redfoxexpand.client.render.AnimatedGuiRenderer;
import redfoxexpand.client.render.AnimationPlaybackCondition;
import redfoxexpand.client.render.GuiTexture;
import redfoxexpand.client.render.StaticGuiTexture;
import net.minecraft.util.ResourceLocation;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Central definition and validation point for all resource-pack paths. */
public final class ResourcePathResolver implements GuiTextureResolver {

    public static final String PHYSICAL_NAMESPACE = "Kyeitk";
    public static final String LOWERCASE_NAMESPACE = "kyeitk";
    public static final String ASSETS_DIRECTORY = "assets";
    public static final String ASSETS_ROOT = ASSETS_DIRECTORY + "/";
    public static final String CONFIG_ROOT = "config/";
    public static final String TEXTURES_ROOT = "textures/";
    public static final String TEXTURE_ROOT = TEXTURES_ROOT + "gui/";
    public static final String COMPATIBILITY_ROOT = "compatibility/";
    public static final String LEGACY_CONFIG_ROOT = "polytone/gui_modifiers/";
    public static final String LEGACY_GUI_SPRITE_ROOT = TEXTURE_ROOT + "sprites/";
    public static final String ANIMATION_FILE = "animation.json";

    private final KyeitkResourceScanner.ResourceIndex resources;
    private final KyeitkTextureRegistry textures;

    public ResourcePathResolver(
            KyeitkResourceScanner.ResourceIndex resources,
            KyeitkTextureRegistry textures
    ) {
        this.resources = resources;
        this.textures = textures;
    }

    @Override
    public GuiTexture resolveStatic(String configuredPath, boolean legacyGuiAtlasId) {
        String canonical = canonicalResourcePath(configuredPath);
        if (canonical == null) {
            return new StaticGuiTexture(
                    LegacyResourceAdapter.resolveTextureLocation(configuredPath, legacyGuiAtlasId)
            );
        }
        if (!canonical.startsWith(TEXTURES_ROOT)
                && !canonical.startsWith(COMPATIBILITY_ROOT)) {
            throw new IllegalArgumentException(
                    "Kyeitk texture must be under textures/ or compatibility/: " + configuredPath
            );
        }
        return new StaticGuiTexture(textures.load(canonical, resources.require(canonical)));
    }

    @Override
    public GuiTexture resolveAnimation(String configuredDirectory) {
        String directory = requireCanonicalPath(configuredDirectory);
        if (directory.endsWith("/")) {
            directory = directory.substring(0, directory.length() - 1);
        }
        String metadataPath = directory + "/" + ANIMATION_FILE;
        AnimationDefinition definition = readAnimation(resources.require(metadataPath));

        String defaultPath = definition.defaultTexture.isEmpty()
                ? directory + ".png"
                : animationAssetPath(directory, definition.defaultTexture);
        ResourceLocation defaultTexture = resolveStatic(defaultPath, false).textureAt(0L);

        List<AnimatedGuiRenderer.Frame> frames = new ArrayList<AnimatedGuiRenderer.Frame>();
        for (AnimationDefinition.Frame frame : definition.frames) {
            try {
                ResourceLocation texture = resolveStatic(
                        animationAssetPath(directory, frame.texture),
                        false
                ).textureAt(0L);
                frames.add(new AnimatedGuiRenderer.Frame(texture, frame.durationMillis));
            } catch (IllegalArgumentException error) {
                if (definition.missingFrameBehavior
                        == AnimationDefinition.MissingFrameBehavior.USE_DEFAULT) {
                    frames.add(new AnimatedGuiRenderer.Frame(defaultTexture, frame.durationMillis));
                } else if (definition.missingFrameBehavior
                        == AnimationDefinition.MissingFrameBehavior.SKIP) {
                    // Missing frames are deliberately omitted from the cached sequence.
                } else {
                    return new StaticGuiTexture(defaultTexture);
                }
            }
        }
        if (frames.isEmpty()) {
            return new StaticGuiTexture(defaultTexture);
        }

        final boolean play = "always".equals(definition.playbackCondition);
        AnimationPlaybackCondition condition = new AnimationPlaybackCondition() {
            @Override
            public boolean shouldPlay() {
                return play;
            }
        };
        return new AnimatedGuiRenderer(
                frames,
                defaultTexture,
                definition.loop,
                condition,
                redfoxexpand.platform.forge1710.Forge1710Clock.INSTANCE.nowMillis()
        );
    }

    private static String animationAssetPath(String directory, String configuredPath) {
        String trimmed = configuredPath.trim();
        if (trimmed.indexOf(':') >= 0
                || trimmed.startsWith(TEXTURES_ROOT)
                || trimmed.startsWith(COMPATIBILITY_ROOT)) {
            return trimmed;
        }
        return directory + "/" + trimmed;
    }

    private static AnimationDefinition readAnimation(
            KyeitkResourceScanner.ResourceFile resource
    ) {
        InputStream stream = null;
        try {
            stream = ResourceLimits.limited(
                    resource.open(),
                    ResourceLimits.MAX_ANIMATION_BYTES,
                    resource.toString()
            );
            Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
            JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
            return AnimationDefinition.parse(json);
        } catch (Exception error) {
            throw new IllegalArgumentException("Invalid GUI animation " + resource, error);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public static String normalizeRelativePath(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Resource path must not be null");
        }
        String normalized = path.trim().replace('\\', '/');
        if (normalized.isEmpty()
                || normalized.startsWith("/")
                || normalized.matches("^[A-Za-z]:.*")) {
            throw new IllegalArgumentException("Resource path must be relative: " + path);
        }
        String[] segments = normalized.split("/");
        StringBuilder result = new StringBuilder();
        for (String segment : segments) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                throw new IllegalArgumentException("Unsafe resource path: " + path);
            }
            if (result.length() > 0) {
                result.append('/');
            }
            result.append(segment);
        }
        return result.toString();
    }

    static String canonicalResourcePath(String configuredPath) {
        if (configuredPath == null) {
            throw new IllegalArgumentException("Texture path must not be null");
        }
        String trimmed = configuredPath.trim();
        if (trimmed.matches("^[A-Za-z]:[\\\\/].*") || trimmed.startsWith("/")) {
            throw new IllegalArgumentException("Absolute texture paths are forbidden: " + configuredPath);
        }
        int separator = trimmed.indexOf(':');
        if (separator < 0) {
            return normalizeRelativePath(trimmed);
        }
        String namespace = trimmed.substring(0, separator);
        if (PHYSICAL_NAMESPACE.equalsIgnoreCase(namespace)) {
            return normalizeRelativePath(trimmed.substring(separator + 1));
        }
        return null;
    }

    private static String requireCanonicalPath(String configuredPath) {
        String canonical = canonicalResourcePath(configuredPath);
        if (canonical == null) {
            throw new IllegalArgumentException(
                    "Animations must be stored under assets/Kyeitk/: " + configuredPath
            );
        }
        return canonical;
    }
}
