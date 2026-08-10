package redfoxexpand.client.gui;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.Identifier;
import redfoxexpand.client.resource.ResourceSnapshot;
import redfoxexpand.client.resource.SnapshotService;
import redfoxexpand.core.GuiContext;
import redfoxexpand.core.GuiDefinition;
import redfoxexpand.core.ResolvedModifier;
import redfoxexpand.mixin.client.AbstractContainerScreenAccessor;
import redfoxexpand.platform.fabric262.Fabric262GuiContextFactory;
import redfoxexpand.platform.fabric262.Fabric262Clock;
import redfoxexpand.platform.fabric262.Fabric262RuntimeStateProvider;
import redfoxexpand.reactive.runtime.RuntimeSnapshot;

import java.util.IdentityHashMap;
import java.util.ArrayList;
import java.util.Map;

/** Owns per-screen lifecycle, transforms and Fabric Screen API hooks. */
public final class ScreenController {
    public static final ScreenController INSTANCE = new ScreenController();
    private final Map<Screen, GuiRuntimeState> states = new IdentityHashMap<>();

    private ScreenController() {
    }

    public void attach(Screen rawScreen) {
        if (!(rawScreen instanceof AbstractContainerScreen<?> screen)) return;
        GuiRuntimeState previous = states.remove(screen);
        if (previous != null) restore(previous);
        GuiRuntimeState state = new GuiRuntimeState(screen);
        states.put(screen, state);
        refresh(state);

        ScreenEvents.beforeExtract(screen).register((ignored, graphics, mouseX, mouseY, tickProgress) -> {
            refresh(state);
            GuiResourceCapture.clear();
        });
        ScreenEvents.afterBackground(screen).register((ignored, graphics, mouseX, mouseY, tickProgress) -> {
            Identifier captured = GuiResourceCapture.finish(screen);
            if (captured != null && !captured.equals(state.currentGuiResource)) {
                state.currentGuiResource = captured;
                refresh(state);
            }
            GuiTextureRenderer.renderLayer(state, graphics, GuiDefinition.Layer.BACKGROUND, mouseX, mouseY);
        });
        ScreenEvents.afterExtract(screen).register((ignored, graphics, mouseX, mouseY, tickProgress) -> GuiResourceCapture.clear());
        ScreenEvents.afterTick(screen).register(ignored -> refresh(state));
        ScreenEvents.remove(screen).register(ignored -> {
            GuiResourceCapture.clear();
            GuiRuntimeState removed = states.remove(screen);
            if (removed != null) restore(removed);
        });
    }

    /** Fabric END_CLIENT_TICK entry: the only place that advances snapshots and derives events. */
    public void clientTick(Minecraft client) {
        for (GuiRuntimeState state : new ArrayList<>(states.values())) {
            refresh(state);
            RuntimeSnapshot runtimeSnapshot = Fabric262RuntimeStateProvider.snapshot(client,
                    state.baseContext, state.modifier.geometry(), state.leftDelta, state.topDelta);
            if (state.playerIdentity != client.player || runtimeSnapshot == null) {
                state.playerIdentity = client.player;
                state.reactiveRuntime = new ReactiveScreenRuntime(state.reactiveCandidates, state.diagnostics);
                state.reactiveRuntime.initialize(runtimeSnapshot, Fabric262Clock.INSTANCE.nowMillis());
                continue;
            }
            state.reactiveRuntime.tick(runtimeSnapshot, Fabric262Clock.INSTANCE.nowMillis());
        }
    }

    public void renderForeground(Screen screen, GuiGraphicsExtractor graphics,
                                 int mouseX, int mouseY) {
        GuiRuntimeState state = states.get(screen);
        if (state != null) {
            GuiTextureRenderer.renderLayer(state, graphics, GuiDefinition.Layer.FOREGROUND, mouseX, mouseY);
        }
    }

    /** Draws above the global dim/blur but below a container's own background. */
    public void renderUnderlayAfterScreenBackground(Screen screen, GuiGraphicsExtractor graphics,
                                                     int mouseX, int mouseY) {
        GuiRuntimeState state = states.get(screen);
        if (state == null) return;
        GuiTextureRenderer.renderLayer(state, graphics, GuiDefinition.Layer.UNDERLAY, mouseX, mouseY);
        // Start only after our underlay so resource_location never captures a RedFoxExpand texture as vanilla GUI input.
        GuiResourceCapture.begin(screen);
    }

    public boolean renderSemanticLabels(AbstractContainerScreen<?> screen,
                                        GuiGraphicsExtractor graphics,
                                        boolean inventoryScreenOverride) {
        GuiRuntimeState state = states.get(screen);
        if (state == null || state.modifier == null || state.modifier.textRules().isEmpty()) return false;
        AbstractContainerScreenAccessor access = (AbstractContainerScreenAccessor) screen;
        renderSemanticLabel(state, graphics, GuiDefinition.TextSelector.TITLE, screen.getTitle(),
                access.redfoxexpand$getTitleLabelX(), access.redfoxexpand$getTitleLabelY());
        if (!inventoryScreenOverride) {
            renderSemanticLabel(state, graphics, GuiDefinition.TextSelector.PLAYER_INVENTORY,
                    access.redfoxexpand$getPlayerInventoryTitle(),
                    access.redfoxexpand$getInventoryLabelX(), access.redfoxexpand$getInventoryLabelY());
        }
        return true;
    }

    private static void renderSemanticLabel(GuiRuntimeState state, GuiGraphicsExtractor graphics,
                                            GuiDefinition.TextSelector selector,
                                            net.minecraft.network.chat.Component text,
                                            int baseX, int baseY) {
        int x = baseX;
        int y = baseY;
        int color = 0xFF404040;
        boolean hidden = false;
        for (GuiDefinition.TextRule rule : state.modifier.textRules()) {
            if (rule.selector() != selector) continue;
            x += rule.xOffset();
            y += rule.yOffset();
            if (rule.color() != null) color = rule.color();
            hidden |= rule.hidden();
        }
        if (!hidden) graphics.text(state.screen.getFont(), text, x, y, color, false);
    }

    private static void refresh(GuiRuntimeState state) {
        restore(state);
        ResourceSnapshot snapshot = SnapshotService.current();
        GuiContext context = Fabric262GuiContextFactory.create(state.screen, state.currentGuiResource);
        ResolvedModifier modifier = ResolvedModifier.resolve(snapshot.definitions(), context);
        boolean runtimeChanged = state.snapshot == null
                || state.snapshot.generation() != snapshot.generation()
                || !state.reactiveCandidates.equals(modifier.matchedDefinitions());
        state.snapshot = snapshot;
        state.baseContext = context;
        state.modifier = modifier;
        if (runtimeChanged) {
            state.reactiveCandidates = modifier.matchedDefinitions();
            state.reactiveRuntime = new ReactiveScreenRuntime(state.reactiveCandidates, state.diagnostics);
            state.playerIdentity = Minecraft.getInstance().player;
        }
        applyGeometry(state, modifier.geometry());
        SlotTransformController.apply(state, modifier, context);
        if (runtimeChanged) {
            state.reactiveRuntime.initialize(Fabric262RuntimeStateProvider.snapshot(Minecraft.getInstance(),
                    context, modifier.geometry(), state.leftDelta, state.topDelta),
                    Fabric262Clock.INSTANCE.nowMillis());
        }
    }

    private static void restore(GuiRuntimeState state) {
        AbstractContainerScreenAccessor access = (AbstractContainerScreenAccessor) state.screen;
        if (state.leftDelta != 0) {
            access.redfoxexpand$setLeftPos(access.redfoxexpand$getLeftPos() - state.leftDelta);
            state.leftDelta = 0;
        }
        if (state.topDelta != 0) {
            access.redfoxexpand$setTopPos(access.redfoxexpand$getTopPos() - state.topDelta);
            state.topDelta = 0;
        }
        SlotTransformController.restore(state);
    }

    private static void applyGeometry(GuiRuntimeState state, GuiDefinition.Geometry geometry) {
        AbstractContainerScreenAccessor access = (AbstractContainerScreenAccessor) state.screen;
        int dx = geometry.xOffset() - geometry.widthOffset() / 2;
        int dy = geometry.yOffset() - geometry.heightOffset() / 2;
        if (dx != 0) access.redfoxexpand$setLeftPos(access.redfoxexpand$getLeftPos() + dx);
        if (dy != 0) access.redfoxexpand$setTopPos(access.redfoxexpand$getTopPos() + dy);
        state.leftDelta = dx;
        state.topDelta = dy;
    }
}
