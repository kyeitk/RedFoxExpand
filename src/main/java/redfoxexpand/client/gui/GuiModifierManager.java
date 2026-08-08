package redfoxexpand.client.gui;

import redfoxexpand.RedFoxExpand;
import redfoxexpand.client.compat.LegacyResourceAdapter;
import redfoxexpand.client.config.GuiConfigLoader;
import redfoxexpand.client.config.DefinitionRegistry;
import redfoxexpand.client.config.GuiDefinition;
import redfoxexpand.client.resource.KyeitkResourceScanner;
import redfoxexpand.client.resource.KyeitkTextureRegistry;
import redfoxexpand.client.resource.NativeManifestScanner;
import redfoxexpand.client.resource.NativeResourcePathResolver;
import redfoxexpand.client.resource.ResourcePackScanner;
import redfoxexpand.client.resource.ResourcePathResolver;
import redfoxexpand.client.resource.ResourceLimits;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import cpw.mods.fml.common.FMLCommonHandler;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class GuiModifierManager {

    private final GuiConfigLoader configLoader = new GuiConfigLoader();
    private volatile List<GuiDefinition> modifiers = Collections.emptyList();
    private KyeitkTextureRegistry textureRegistry;

    public synchronized void reload(IResourceManager resourceManager) throws Exception {
        if (!(resourceManager instanceof SimpleReloadableResourceManager)) {
            throw new IllegalArgumentException("Unsupported resource manager: " + resourceManager.getClass());
        }

        SimpleReloadableResourceManager reloadable =
                (SimpleReloadableResourceManager) resourceManager;
        KyeitkResourceScanner.ResourceIndex index = KyeitkResourceScanner.scan(reloadable);
        List<KyeitkResourceScanner.ResourceFile> canonicalConfigs = index.guiConfigs();
        List<ResourceLocation> legacyConfigs = ResourcePackScanner.findGuiModifiers(reloadable);
        List<ResourceLocation> nativeConfigs = NativeManifestScanner.findConfigs(resourceManager);
        List<GuiDefinition> next = new ArrayList<GuiDefinition>();
        KyeitkTextureRegistry nextTextures = new KyeitkTextureRegistry(
                Minecraft.getMinecraft().getTextureManager()
        );
        try {
            // Formats are candidates, not a global switch. A bad or unrelated
            // Kyeitk file must never suppress valid legacy definitions.
            loadLegacy(legacyConfigs, resourceManager, next);
            if (!canonicalConfigs.isEmpty()) {
                ResourcePathResolver resolver = new ResourcePathResolver(index, nextTextures);
                loadCanonical(canonicalConfigs, resolver, next);
            }
            loadNative(
                    nativeConfigs,
                    resourceManager,
                    new NativeResourcePathResolver(resourceManager),
                    next
            );
        } catch (Throwable error) {
            nextTextures.close();
            if (error instanceof Error) {
                throw (Error) error;
            }
            if (error instanceof Exception) {
                throw (Exception) error;
            }
            throw new Exception(error);
        }

        KyeitkTextureRegistry previousTextures = textureRegistry;
        textureRegistry = nextTextures;
        next = DefinitionRegistry.resolve(next);
        modifiers = next;
        refreshCurrentScreen();
        if (previousTextures != null) {
            previousTextures.close();
        }
        RedFoxExpand.LOGGER.info(
                "Loaded {} GUI modifier(s) from {} legacy, {} Kyeitk v1 and {} native v2 path(s)",
                next.size(),
                legacyConfigs.size(),
                canonicalConfigs.size(),
                nativeConfigs.size()
        );
    }

    private void loadCanonical(
            List<KyeitkResourceScanner.ResourceFile> configs,
            ResourcePathResolver resolver,
            List<GuiDefinition> result
    ) {
        for (KyeitkResourceScanner.ResourceFile resource : configs) {
            InputStream stream = null;
            try {
                stream = ResourceLimits.limited(
                        resource.open(),
                        ResourceLimits.MAX_CONFIG_BYTES,
                        resource.toString()
                );
                Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
                result.addAll(configLoader.load(
                        new ResourceLocation(ResourcePathResolver.LOWERCASE_NAMESPACE, resource.path),
                        reader,
                        resolver
                ));
            } catch (Exception error) {
                RedFoxExpand.LOGGER.error("Invalid Kyeitk GUI config {}", resource, error);
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    private void loadLegacy(
            List<ResourceLocation> locations,
            IResourceManager resourceManager,
            List<GuiDefinition> result
    ) {
        for (ResourceLocation location : locations) {
            InputStream stream = null;
            try {
                List<IResource> stack = resourceManager.getAllResources(location);
                if (stack.isEmpty()) {
                    continue;
                }
                stream = ResourceLimits.limited(
                        stack.get(stack.size() - 1).getInputStream(),
                        ResourceLimits.MAX_CONFIG_BYTES,
                        location.toString()
                );
                Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
                result.addAll(configLoader.load(location, reader, LegacyResourceAdapter.INSTANCE));
            } catch (Exception error) {
                RedFoxExpand.LOGGER.error("Invalid legacy GUI modifier {}", location, error);
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    private void loadNative(
            List<ResourceLocation> locations,
            IResourceManager resourceManager,
            NativeResourcePathResolver resolver,
            List<GuiDefinition> result
    ) {
        for (ResourceLocation location : locations) {
            InputStream stream = null;
            try {
                List<IResource> stack = resourceManager.getAllResources(location);
                if (stack.isEmpty()) {
                    continue;
                }
                stream = ResourceLimits.limited(
                        stack.get(stack.size() - 1).getInputStream(),
                        ResourceLimits.MAX_CONFIG_BYTES,
                        location.toString()
                );
                result.addAll(configLoader.load(
                        location,
                        new InputStreamReader(stream, StandardCharsets.UTF_8),
                        resolver
                ));
            } catch (Exception error) {
                RedFoxExpand.LOGGER.error("Invalid native Kyeitk v2 config {}", location, error);
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    public synchronized void clearAndRefresh() {
        modifiers = Collections.emptyList();
        if (textureRegistry != null) {
            textureRegistry.close();
            textureRegistry = null;
        }
        refreshCurrentScreen();
    }

    public ResolvedGuiModifier resolve(GuiContainer screen) {
        List<GuiDefinition> matches = collectMatches(
                modifiers,
                screen.getClass(),
                screen.inventorySlots.getClass(),
                GuiTitleResolver.resolve(screen)
        );
        return matches.isEmpty() ? null : ResolvedGuiModifier.merge(matches);
    }

    private ResolvedGuiModifier resolve(Container container) {
        List<GuiDefinition> matches = matchingClass(
                modifiers,
                GuiDefinition.TargetType.CONTAINER_CLASS,
                container.getClass()
        );
        return matches.isEmpty() ? null : ResolvedGuiModifier.merge(matches);
    }

    static List<GuiDefinition> collectMatches(
            List<GuiDefinition> definitions,
            Class<?> screenClass,
            Class<?> containerClass,
            Set<String> titles
    ) {
        List<GuiDefinition> result = new ArrayList<GuiDefinition>();
        result.addAll(matchingTitles(definitions, titles));
        result.addAll(matchingClass(
                definitions,
                GuiDefinition.TargetType.CONTAINER_CLASS,
                containerClass
        ));
        result.addAll(matchingClass(
                definitions,
                GuiDefinition.TargetType.SCREEN_CLASS,
                screenClass
        ));
        return result;
    }

    private static List<GuiDefinition> matchingClass(
            List<GuiDefinition> definitions,
            GuiDefinition.TargetType type,
            Class<?> targetClass
    ) {
        List<GuiDefinition> result = new ArrayList<GuiDefinition>();
        for (GuiDefinition modifier : definitions) {
            if (modifier.targetType == type
                    && ClassNameMatcher.matches(
                            modifier.target,
                            targetClass,
                            modifier.classMatch
                    )) {
                result.add(modifier);
            }
        }
        return result;
    }

    private static List<GuiDefinition> matchingTitles(
            List<GuiDefinition> definitions,
            Set<String> titles
    ) {
        List<GuiDefinition> result = new ArrayList<GuiDefinition>();
        for (GuiDefinition modifier : definitions) {
            if (modifier.targetType != GuiDefinition.TargetType.SCREEN_TITLE) {
                continue;
            }
            for (String title : titles) {
                if (matchesText(modifier.target, title)) {
                    result.add(modifier);
                    break;
                }
            }
        }
        return result;
    }

    private static boolean matchesText(String configured, String actual) {
        if (configured.equals(actual)) {
            return true;
        }
        if (configured.indexOf('*') < 0) {
            return false;
        }
        StringBuilder regex = new StringBuilder();
        String[] parts = configured.split("\\*", -1);
        for (int index = 0; index < parts.length; index++) {
            if (index > 0) {
                regex.append(".*");
            }
            regex.append(Pattern.quote(parts[index]));
        }
        return actual.matches(regex.toString());
    }

    public void onSlotAdded(Container container, Slot slot) {
        if (!FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            return;
        }
        if (!(slot instanceof SlotBaseAccess)) {
            return;
        }
        ResolvedGuiModifier modifier = resolve(container);
        resetAndApply(container, slot, modifier);
    }

    public void onPostInit(GuiContainer screen) {
        if (screen instanceof GuiModifierScreenAccess) {
            ((GuiModifierScreenAccess) screen).redfoxexpand$onPostInit();
        }
    }

    public void applyAllSlots(GuiContainer screen, ResolvedGuiModifier modifier) {
        for (Object candidate : screen.inventorySlots.inventorySlots) {
            resetAndApply(screen.inventorySlots, (Slot) candidate, modifier);
        }
    }

    static void resetAndApply(
            Container container,
            Slot slot,
            ResolvedGuiModifier modifier
    ) {
        if (!(slot instanceof SlotBaseAccess)) {
            return;
        }
        SlotBaseAccess base = (SlotBaseAccess) slot;
        base.redfoxexpand$removeAppliedDelta();
        if (modifier == null) {
            return;
        }
        for (SlotModifier slotModifier : modifier.matchingSlots(container, slot)) {
            slotModifier.apply(slot);
            base.redfoxexpand$recordAppliedDelta(
                    slotModifier.xOffset,
                    slotModifier.yOffset
            );
        }
    }

    public SlotModifier highlightFor(
            GuiContainer screen,
            ResolvedGuiModifier modifier,
            Slot slot
    ) {
        if (modifier == null || slot == null) {
            return null;
        }
        for (SlotModifier candidate : modifier.matchingSlots(screen.inventorySlots, slot)) {
            if (candidate.hasHighlight()) {
                return candidate;
            }
        }
        return null;
    }

    private void refreshCurrentScreen() {
        GuiScreen current = Minecraft.getMinecraft().currentScreen;
        if (current instanceof GuiContainer && current instanceof GuiModifierScreenAccess) {
            ((GuiModifierScreenAccess) current).redfoxexpand$refreshModifier();
        }
    }
}
