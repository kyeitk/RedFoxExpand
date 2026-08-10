package redfoxexpand.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.FMLCommonHandler;
import redfoxexpand.RedFoxExpand;
import redfoxexpand.client.compat.LegacyResourceAdapter;
import redfoxexpand.client.config.GuiConfigLoader;
import redfoxexpand.client.config.GuiDefinition;
import redfoxexpand.client.resource.KyeitkResourceScanner;
import redfoxexpand.client.resource.KyeitkTextureRegistry;
import redfoxexpand.client.resource.NativeManifestScanner;
import redfoxexpand.client.resource.NativeTextureResolver;
import redfoxexpand.client.resource.ResourcePackScanner;
import redfoxexpand.client.resource.ResourcePathResolver;
import redfoxexpand.client.render.GuiTexture;
import redfoxexpand.core.DefinitionCandidate;
import redfoxexpand.core.DefinitionRegistry;
import redfoxexpand.core.SchemaV2Parser;
import redfoxexpand.core.SchemaV3Parser;
import redfoxexpand.reactive.runtime.Capability;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Atomic multi-schema registry and resolver for one 1.8.9 client. */
public final class GuiModifierManager {
    private final GuiConfigLoader configLoader = new GuiConfigLoader();
    private final SchemaV2Parser schemaV2 = new SchemaV2Parser(false);
    private final SchemaV3Parser schemaV3 = new SchemaV3Parser(
            EnumSet.allOf(Capability.class), false);
    private volatile Snapshot snapshot = Snapshot.EMPTY;
    private long nextGeneration;

    public synchronized void reload(IResourceManager resourceManager) throws Exception {
        if (!(resourceManager instanceof SimpleReloadableResourceManager)) {
            throw new IllegalArgumentException("Unsupported resource manager: " + resourceManager.getClass());
        }
        SimpleReloadableResourceManager reloadable = (SimpleReloadableResourceManager) resourceManager;
        KyeitkResourceScanner.ResourceIndex index = KyeitkResourceScanner.scan(reloadable);
        List<KyeitkResourceScanner.ResourceFile> canonicalConfigs = index.guiConfigs();
        List<ResourceLocation> legacyConfigs = Collections.emptyList();
        List<GuiDefinition> legacy = new ArrayList<GuiDefinition>();
        KyeitkTextureRegistry nextTextures = new KyeitkTextureRegistry(
                Minecraft.getMinecraft().getTextureManager());

        try {
            if (!canonicalConfigs.isEmpty()) {
                loadCanonical(canonicalConfigs, new ResourcePathResolver(index, nextTextures), legacy);
            } else {
                legacyConfigs = ResourcePackScanner.findGuiModifiers(reloadable);
                loadLegacy(legacyConfigs, resourceManager, legacy);
            }

            NativeLoad nativeLoad = loadNative(resourceManager,
                    NativeManifestScanner.findConfigs(resourceManager));
            Snapshot next = new Snapshot(++nextGeneration,
                    Collections.unmodifiableList(new ArrayList<GuiDefinition>(legacy)),
                    nativeLoad.active, nativeLoad.rendered, nextTextures);
            Snapshot previous = snapshot;
            snapshot = next;
            refreshCurrentScreen();
            previous.closeTextures();
            RedFoxExpand.LOGGER.info(
                    "Loaded generation {}: {} v1/legacy and {} native Schema v2/v3 GUI definitions",
                    next.generation, next.legacy.size(), next.nativeDefinitions.size());
        } catch (Throwable error) {
            nextTextures.close();
            if (error instanceof Error) throw (Error) error;
            if (error instanceof Exception) throw (Exception) error;
            throw new Exception(error);
        }
    }

    private NativeLoad loadNative(IResourceManager manager,
                                  List<NativeManifestScanner.ConfigRef> configs) {
        List<DefinitionCandidate> candidates = new ArrayList<DefinitionCandidate>();
        Map<DefinitionCandidate, GuiDefinition> rendered =
                new HashMap<DefinitionCandidate, GuiDefinition>();
        NativeTextureResolver textureResolver = new NativeTextureResolver(manager);
        for (NativeManifestScanner.ConfigRef ref : configs) {
            InputStream stream = null;
            try {
                IResource resource = NativeManifestScanner.samePackResource(manager, ref);
                stream = NativeManifestScanner.limited(resource.getInputStream(),
                        redfoxexpand.core.ResourceLimits.MAX_JSON_BYTES, ref.toString());
                Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
                List<SchemaV2Parser.ParsedDefinition> parsed = ref.apiVersion == 2
                        ? schemaV2.parse(reader, ref.toString())
                        : schemaV3.parse(reader, ref.toString());
                for (int index = 0; index < parsed.size(); index++) {
                    SchemaV2Parser.ParsedDefinition item = parsed.get(index);
                    DefinitionCandidate candidate = new DefinitionCandidate(
                            item.id(), ref.apiVersion, ref.sourcePack, ref.sourcePriority,
                            ref.location.toString(), index, item.matcher(), item.operation(),
                            item.priority(), item.definition());
                    try {
                        List<SpriteOverlay> sprites = new ArrayList<SpriteOverlay>();
                        for (redfoxexpand.core.GuiDefinition.Sprite sprite : item.definition().sprites()) {
                            GuiTexture texture = textureResolver.resolve(sprite);
                            sprites.add(SpriteOverlay.fromNative(sprite, texture, candidate));
                        }
                        candidates.add(candidate);
                        rendered.put(candidate, GuiDefinition.fromNative(candidate, sprites));
                    } catch (Exception error) {
                        RedFoxExpand.LOGGER.error("Rejected native definition {} from {}",
                                item.id(), ref, error);
                    }
                }
            } catch (Exception error) {
                RedFoxExpand.LOGGER.error("Invalid native Schema v{} config {}",
                        ref.apiVersion, ref, error);
            } finally {
                if (stream != null) try { stream.close(); } catch (Exception ignored) { }
            }
        }
        List<DefinitionCandidate> active = DefinitionRegistry.resolve(candidates);
        Map<DefinitionCandidate, GuiDefinition> activeRendered =
                new HashMap<DefinitionCandidate, GuiDefinition>();
        for (DefinitionCandidate candidate : active) {
            GuiDefinition definition = rendered.get(candidate);
            if (definition != null) activeRendered.put(candidate, definition);
        }
        return new NativeLoad(active, Collections.unmodifiableMap(activeRendered));
    }

    private void loadCanonical(List<KyeitkResourceScanner.ResourceFile> configs,
                               ResourcePathResolver resolver, List<GuiDefinition> result) {
        for (KyeitkResourceScanner.ResourceFile resource : configs) {
            InputStream stream = null;
            try {
                stream = resource.open();
                result.addAll(configLoader.load(
                        new ResourceLocation(ResourcePathResolver.LOWERCASE_NAMESPACE, resource.path),
                        new InputStreamReader(stream, StandardCharsets.UTF_8), resolver));
            } catch (Exception error) {
                RedFoxExpand.LOGGER.error("Invalid Kyeitk GUI config {}", resource, error);
            } finally {
                if (stream != null) try { stream.close(); } catch (Exception ignored) { }
            }
        }
    }

    private void loadLegacy(List<ResourceLocation> locations, IResourceManager resourceManager,
                            List<GuiDefinition> result) {
        for (ResourceLocation location : locations) {
            InputStream stream = null;
            try {
                List<IResource> stack = resourceManager.getAllResources(location);
                if (stack.isEmpty()) continue;
                stream = stack.get(stack.size() - 1).getInputStream();
                result.addAll(configLoader.load(location,
                        new InputStreamReader(stream, StandardCharsets.UTF_8),
                        LegacyResourceAdapter.INSTANCE));
            } catch (Exception error) {
                RedFoxExpand.LOGGER.error("Invalid legacy GUI modifier {}", location, error);
            } finally {
                if (stream != null) try { stream.close(); } catch (Exception ignored) { }
            }
        }
    }

    public synchronized void clearAndRefresh() {
        Snapshot previous = snapshot;
        snapshot = Snapshot.EMPTY;
        refreshCurrentScreen();
        previous.closeTextures();
    }

    public long generation() { return snapshot.generation; }

    public ResolvedGuiModifier resolve(GuiContainer screen, int guiLeft, int guiTop,
                                       int guiWidth, int guiHeight) {
        Snapshot current = snapshot;
        List<GuiDefinition> matches = collectMatches(current.legacy, screen.getClass(),
                screen.inventorySlots.getClass(), GuiTitleResolver.resolve(screen));
        redfoxexpand.core.ResolvedModifier nativeResolved = redfoxexpand.core.ResolvedModifier.resolve(
                current.nativeDefinitions,
                GuiContextFactory189.create(screen, guiLeft, guiTop, guiWidth, guiHeight));
        for (DefinitionCandidate candidate : nativeResolved.matchedDefinitions()) {
            GuiDefinition rendered = current.nativeRendered.get(candidate);
            if (rendered != null) matches.add(rendered);
        }
        return matches.isEmpty() ? null : ResolvedGuiModifier.merge(matches);
    }

    private ResolvedGuiModifier resolve(Container container) {
        List<GuiDefinition> matches = matchingClass(snapshot.legacy,
                GuiDefinition.TargetType.CONTAINER_CLASS, container.getClass());
        return matches.isEmpty() ? null : ResolvedGuiModifier.merge(matches);
    }

    static List<GuiDefinition> collectMatches(List<GuiDefinition> definitions,
                                               Class<?> screenClass, Class<?> containerClass,
                                               Set<String> titles) {
        List<GuiDefinition> result = new ArrayList<GuiDefinition>();
        result.addAll(matchingTitles(definitions, titles));
        result.addAll(matchingClass(definitions, GuiDefinition.TargetType.CONTAINER_CLASS, containerClass));
        result.addAll(matchingClass(definitions, GuiDefinition.TargetType.SCREEN_CLASS, screenClass));
        return result;
    }

    private static List<GuiDefinition> matchingClass(List<GuiDefinition> definitions,
                                                      GuiDefinition.TargetType type,
                                                      Class<?> targetClass) {
        List<GuiDefinition> result = new ArrayList<GuiDefinition>();
        for (GuiDefinition modifier : definitions) {
            if (modifier.targetType == type && ClassNameMatcher.matches(
                    modifier.target, targetClass, modifier.classMatch)) result.add(modifier);
        }
        return result;
    }

    private static List<GuiDefinition> matchingTitles(List<GuiDefinition> definitions,
                                                       Set<String> titles) {
        List<GuiDefinition> result = new ArrayList<GuiDefinition>();
        for (GuiDefinition modifier : definitions) {
            if (modifier.targetType != GuiDefinition.TargetType.SCREEN_TITLE) continue;
            for (String title : titles) {
                if (matchesText(modifier.target, title)) { result.add(modifier); break; }
            }
        }
        return result;
    }

    private static boolean matchesText(String configured, String actual) {
        if (configured.equals(actual)) return true;
        if (configured.indexOf('*') < 0) return false;
        StringBuilder regex = new StringBuilder();
        String[] parts = configured.split("\\*", -1);
        for (int index = 0; index < parts.length; index++) {
            if (index > 0) regex.append(".*");
            regex.append(Pattern.quote(parts[index]));
        }
        return actual.matches(regex.toString());
    }

    public void onSlotAdded(Container container, Slot slot) {
        if (!FMLCommonHandler.instance().getEffectiveSide().isClient()) return;
        resetAndApply(container, slot, resolve(container));
    }
    public void onPostInit(GuiContainer screen) {
        ((GuiModifierScreenAccess) screen).redfoxexpand$onPostInit();
    }
    public void applyAllSlots(GuiContainer screen, ResolvedGuiModifier modifier) {
        for (Slot slot : screen.inventorySlots.inventorySlots) resetAndApply(screen.inventorySlots, slot, modifier);
    }
    private static void resetAndApply(Container container, Slot slot, ResolvedGuiModifier modifier) {
        SlotBaseAccess base = (SlotBaseAccess) slot;
        base.redfoxexpand$captureBase(); base.redfoxexpand$resetToBase();
        if (modifier == null) return;
        for (SlotModifier rule : modifier.matchingSlots(container, slot)) rule.apply(slot);
    }
    public SlotModifier highlightFor(GuiContainer screen, ResolvedGuiModifier modifier, Slot slot) {
        if (modifier == null || slot == null) return null;
        for (SlotModifier candidate : modifier.matchingSlots(screen.inventorySlots, slot)) {
            if (candidate.hasHighlight()) return candidate;
        }
        return null;
    }
    private void refreshCurrentScreen() {
        GuiScreen current = Minecraft.getMinecraft().currentScreen;
        if (current instanceof GuiContainer && current instanceof GuiModifierScreenAccess) {
            ((GuiModifierScreenAccess) current).redfoxexpand$refreshModifier();
        }
    }

    private static final class NativeLoad {
        final List<DefinitionCandidate> active;
        final Map<DefinitionCandidate, GuiDefinition> rendered;
        NativeLoad(List<DefinitionCandidate> active, Map<DefinitionCandidate, GuiDefinition> rendered) {
            this.active = active; this.rendered = rendered;
        }
    }
    private static final class Snapshot {
        static final Snapshot EMPTY = new Snapshot(0L, Collections.<GuiDefinition>emptyList(),
                Collections.<DefinitionCandidate>emptyList(),
                Collections.<DefinitionCandidate, GuiDefinition>emptyMap(), null);
        final long generation;
        final List<GuiDefinition> legacy;
        final List<DefinitionCandidate> nativeDefinitions;
        final Map<DefinitionCandidate, GuiDefinition> nativeRendered;
        final KyeitkTextureRegistry textures;
        Snapshot(long generation, List<GuiDefinition> legacy,
                 List<DefinitionCandidate> nativeDefinitions,
                 Map<DefinitionCandidate, GuiDefinition> nativeRendered,
                 KyeitkTextureRegistry textures) {
            this.generation = generation; this.legacy = legacy;
            this.nativeDefinitions = nativeDefinitions; this.nativeRendered = nativeRendered;
            this.textures = textures;
        }
        void closeTextures() { if (textures != null) textures.close(); }
    }
}
