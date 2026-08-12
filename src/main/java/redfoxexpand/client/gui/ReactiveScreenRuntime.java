package redfoxexpand.client.gui;

import redfoxexpand.core.DefinitionCandidate;
import redfoxexpand.core.GuiDefinition;
import redfoxexpand.reactive.ReactiveDefinition;
import redfoxexpand.reactive.animation.AnimationController;
import redfoxexpand.reactive.animation.AnimationProperties;
import redfoxexpand.reactive.animation.PropertyAnimation;
import redfoxexpand.reactive.behavior.Action;
import redfoxexpand.reactive.behavior.ActionSink;
import redfoxexpand.reactive.behavior.BehaviorEngine;
import redfoxexpand.reactive.behavior.PlayAnimationAction;
import redfoxexpand.reactive.behavior.SetAlphaAction;
import redfoxexpand.reactive.behavior.SetVisibleAction;
import redfoxexpand.reactive.behavior.StopAnimationAction;
import redfoxexpand.reactive.binding.Binding;
import redfoxexpand.reactive.binding.NumericSmoother;
import redfoxexpand.reactive.binding.ReactiveProperty;
import redfoxexpand.reactive.event.EventDetector;
import redfoxexpand.reactive.event.RuntimeEvent;
import redfoxexpand.reactive.expression.DerivedValue;
import redfoxexpand.reactive.property.FinalRenderProperties;
import redfoxexpand.reactive.property.PropertyPipeline;
import redfoxexpand.reactive.runtime.RuntimeContext;
import redfoxexpand.reactive.runtime.RuntimeDiagnostics;
import redfoxexpand.reactive.runtime.RuntimeSnapshot;
import redfoxexpand.reactive.value.RuntimeValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Per-screen mutable v3 state. It owns no Screen, Player, World, or resource-manager reference. */
public final class ReactiveScreenRuntime {
    private final EventDetector eventDetector = new EventDetector();
    private final AnimationController animations = new AnimationController();
    private final List<DefinitionRuntime> definitions = new ArrayList<>();
    private final Map<GuiDefinition.Sprite, ElementState> elements = new IdentityHashMap<>();
    private final Map<GuiDefinition.Sprite, DefinitionRuntime> owners = new IdentityHashMap<>();
    private RuntimeSnapshot previous;
    private RuntimeSnapshot current;

    public ReactiveScreenRuntime(List<DefinitionCandidate> candidates, RuntimeDiagnostics diagnostics) {
        for (DefinitionCandidate candidate : candidates) {
            if (candidate.apiVersion() != 3 && candidate.apiVersion() != 31) continue;
            DefinitionRuntime runtime = new DefinitionRuntime(candidate, diagnostics);
            definitions.add(runtime);
            elements.putAll(runtime.bySprite);
            for (GuiDefinition.Sprite sprite : runtime.bySprite.keySet()) owners.put(sprite, runtime);
        }
    }

    public void initialize(RuntimeSnapshot snapshot) {
        initialize(snapshot, 0L);
    }

    public void initialize(RuntimeSnapshot snapshot, long nowMillis) {
        previous = snapshot;
        current = snapshot;
        if (snapshot != null) {
            for (DefinitionRuntime definition : definitions) definition.evaluateBindings(snapshot, nowMillis);
            List<RuntimeEvent> opened = Collections.singletonList(
                    RuntimeEvent.state(RuntimeEvent.SCREEN_OPENED));
            for (DefinitionRuntime definition : definitions) {
                definition.process(opened, snapshot, animations, nowMillis);
            }
        }
    }

    public void tick(RuntimeSnapshot snapshot, long nowMillis) {
        if (snapshot == null) {
            previous = null;
            current = null;
            animations.clear();
            for (DefinitionRuntime definition : definitions) definition.resetBindings();
            return;
        }
        List<RuntimeEvent> events = eventDetector.detect(previous, snapshot);
        previous = snapshot;
        current = snapshot;
        for (DefinitionRuntime definition : definitions) definition.evaluateBindings(snapshot, nowMillis);
        for (DefinitionRuntime definition : definitions) definition.process(events, snapshot, animations, nowMillis);
    }

    public FinalRenderProperties properties(GuiDefinition.Sprite sprite, long nowMillis) {
        ElementState state = elements.get(sprite);
        if (state == null) return FinalRenderProperties.BASE;
        return properties(state, nowMillis);
    }

    /** Compatibility entry point retained for the existing 1.8.9 render adapter/tests. */
    public FinalRenderProperties properties(SpriteOverlay sprite, long nowMillis) {
        return sprite == null || sprite.nativeSprite == null
                ? FinalRenderProperties.BASE : properties(sprite.nativeSprite, nowMillis);
    }

    public SceneRenderState scene(GuiDefinition.Sprite sprite, long nowMillis) {
        DefinitionRuntime owner = owners.get(sprite);
        ElementState leaf = elements.get(sprite);
        if (owner == null || leaf == null || !sprite.sceneManaged()) return null;
        List<ElementState> reverse = new ArrayList<ElementState>();
        ElementState cursor = leaf;
        while (cursor != null) {
            reverse.add(cursor);
            cursor = cursor.parentId == null ? null : owner.byId.get(cursor.parentId);
        }
        List<SceneRenderState.Node> nodes = new ArrayList<SceneRenderState.Node>();
        for (int index = reverse.size() - 1; index >= 0; index--) {
            ElementState state = reverse.get(index);
            nodes.add(new SceneRenderState.Node(state.anchor, state.x, state.y, state.pivot,
                    properties(state, nowMillis)));
        }
        return new SceneRenderState(nodes);
    }

    public RuntimeSnapshot currentSnapshot() {
        return current;
    }

    public void clear() {
        previous = null;
        current = null;
        animations.clear();
        for (DefinitionRuntime definition : definitions) definition.resetBindings();
    }

    private FinalRenderProperties properties(ElementState state, long nowMillis) {
        AnimationProperties animation = animations.evaluate(state.scopedTarget, nowMillis);
        return PropertyPipeline.resolve(state.boundVisible,
                state.number(ReactiveProperty.ALPHA, nowMillis),
                state.number(ReactiveProperty.TRANSLATE_X, nowMillis),
                state.number(ReactiveProperty.TRANSLATE_Y, nowMillis),
                state.number(ReactiveProperty.SCALE_X, nowMillis),
                state.number(ReactiveProperty.SCALE_Y, nowMillis),
                state.number(ReactiveProperty.ROTATION_Z, nowMillis), animation,
                state.overrideVisible, state.overrideAlpha);
    }

    private static final class DefinitionRuntime {
        final ReactiveDefinition definition;
        final Map<String, ElementState> byId = new LinkedHashMap<>();
        final Map<GuiDefinition.Sprite, ElementState> bySprite = new IdentityHashMap<>();
        final BehaviorEngine behaviors;
        final RuntimeDiagnostics diagnostics;

        DefinitionRuntime(DefinitionCandidate candidate, RuntimeDiagnostics diagnostics) {
            this.definition = candidate.definition().reactive();
            this.diagnostics = diagnostics;
            String scope = candidate.sourcePack() + "\u0000" + candidate.sourcePath() + "\u0000"
                    + candidate.sourceIndex() + "\u0000" + candidate.id();
            for (GuiDefinition.Group group : candidate.definition().groups()) {
                ElementState state = new ElementState(scope + "\u0000" + group.id(), group.id(),
                        group.parentId(), group.anchor(), group.x(), group.y(), group.width(),
                        group.height(), group.pivot());
                byId.put(group.id(), state);
            }
            for (GuiDefinition.Sprite sprite : candidate.definition().sprites()) {
                if (sprite.id() == null) continue;
                ElementState state = new ElementState(scope + "\u0000" + sprite.id(), sprite.id(),
                        sprite.parentId(), sprite.anchor(), sprite.x(), sprite.y(), sprite.width(),
                        sprite.height(), sprite.pivot());
                byId.put(sprite.id(), state);
                bySprite.put(sprite, state);
            }
            final String diagnosticScope = scope;
            this.behaviors = new BehaviorEngine(definition.getBehaviors(), diagnostics == null ? null :
                    new RuntimeDiagnostics() {
                        @Override
                        public void warning(String key, String message, Throwable error) {
                            diagnostics.warning(diagnosticScope + "-" + key, message, error);
                        }
                    });
        }

        void evaluateBindings(RuntimeSnapshot snapshot, long nowMillis) {
            for (ElementState state : byId.values()) state.beginBindingPass();
            RuntimeContext shared = definitionContext(snapshot);
            for (Binding binding : definition.getBindings()) {
                ElementState target = byId.get(binding.getTarget());
                if (target == null) continue;
                try {
                    RuntimeValue value = binding.getExpression().evaluate(
                            new ElementContext(shared, target, byId));
                    if (binding.getProperty() == ReactiveProperty.VISIBLE) target.boundVisible = value.asBoolean();
                    else target.bindNumber(binding.getProperty(), value.asNumber(),
                            binding.getSmoothingMillis(), nowMillis);
                } catch (RuntimeException error) {
                    if (diagnostics != null) diagnostics.warning(
                            target.scopedTarget + "-binding-" + binding.getProperty().getSchemaName(),
                            "Schema v3 binding runtime failure; base value used", error);
                }
            }
        }

        void resetBindings() {
            for (ElementState state : byId.values()) state.resetBindings();
        }

        void process(List<RuntimeEvent> events, RuntimeSnapshot snapshot,
                     final AnimationController controller, final long nowMillis) {
            behaviors.process(events, definitionContext(snapshot), new ActionSink() {
                @Override
                public void execute(Action action, RuntimeEvent event, RuntimeContext context) {
                    if (action instanceof PlayAnimationAction) {
                        PlayAnimationAction play = (PlayAnimationAction) action;
                        PropertyAnimation animation = definition.getAnimations().get(play.getAnimation());
                        controller.play(byId.get(play.getTarget()).scopedTarget, animation,
                                nowMillis, play.isRestart());
                    } else if (action instanceof StopAnimationAction) {
                        StopAnimationAction stop = (StopAnimationAction) action;
                        controller.stop(byId.get(stop.getTarget()).scopedTarget, stop.getAnimation());
                    } else if (action instanceof SetVisibleAction) {
                        SetVisibleAction visible = (SetVisibleAction) action;
                        byId.get(visible.getTarget()).overrideVisible = visible.getValue();
                    } else if (action instanceof SetAlphaAction) {
                        SetAlphaAction alpha = (SetAlphaAction) action;
                        byId.get(alpha.getTarget()).overrideAlpha = alpha.getValue();
                    }
                }
            });
        }

        private RuntimeContext definitionContext(final RuntimeSnapshot snapshot) {
            final Map<String, RuntimeValue> values = new LinkedHashMap<String, RuntimeValue>(
                    definition.getConstants());
            RuntimeContext context = new RuntimeContext() {
                @Override
                public RuntimeValue get(String name) {
                    RuntimeValue local = values.get(name);
                    return local == null ? snapshot.get(name) : local;
                }
            };
            for (DerivedValue value : definition.getValues()) {
                try {
                    values.put(value.getName(), value.getExpression().evaluate(context));
                } catch (RuntimeException error) {
                    if (diagnostics != null) diagnostics.warning(
                            "derived-" + value.getName(),
                            "Schema v3.1 derived value runtime failure", error);
                }
            }
            return context;
        }
    }

    private static final class ElementState {
        final String scopedTarget;
        final String id;
        final String parentId;
        final GuiDefinition.Anchor anchor;
        final double x;
        final double y;
        final double width;
        final double height;
        final GuiDefinition.Pivot pivot;
        final Map<ReactiveProperty, NumericBindingState> numericBindings =
                new EnumMap<ReactiveProperty, NumericBindingState>(ReactiveProperty.class);
        Boolean boundVisible;
        Boolean overrideVisible;
        Double overrideAlpha;

        ElementState(String scopedTarget, String id, String parentId, GuiDefinition.Anchor anchor,
                     double x, double y, double width, double height, GuiDefinition.Pivot pivot) {
            this.scopedTarget = scopedTarget;
            this.id = id;
            this.parentId = parentId;
            this.anchor = anchor;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.pivot = pivot;
        }

        void beginBindingPass() {
            boundVisible = null;
            for (NumericBindingState state : numericBindings.values()) state.active = false;
        }

        void bindNumber(ReactiveProperty property, double value, int smoothingMillis, long nowMillis) {
            NumericBindingState state = numericBindings.get(property);
            if (state == null) {
                state = new NumericBindingState();
                numericBindings.put(property, state);
            }
            state.smoother.update(value, nowMillis, smoothingMillis);
            state.active = true;
        }

        Double number(ReactiveProperty property, long nowMillis) {
            NumericBindingState state = numericBindings.get(property);
            return state == null || !state.active ? null : Double.valueOf(state.smoother.sample(nowMillis));
        }

        void resetBindings() {
            boundVisible = null;
            numericBindings.clear();
        }
    }

    private static final class NumericBindingState {
        final NumericSmoother smoother = new NumericSmoother();
        boolean active;
    }

    private static final class ElementContext implements RuntimeContext {
        private final RuntimeContext parent;
        private final ElementState self;
        private final Map<String, ElementState> elements;

        ElementContext(RuntimeContext parent, ElementState self, Map<String, ElementState> elements) {
            this.parent = parent;
            this.self = self;
            this.elements = elements;
        }

        @Override
        public RuntimeValue get(String name) {
            if (name.startsWith("self.")) return geometry(self, name.substring(5));
            if (name.startsWith("parent.")) {
                ElementState value = self.parentId == null ? null : elements.get(self.parentId);
                return value == null ? null : geometry(value, name.substring(7));
            }
            return parent.get(name);
        }

        private RuntimeValue geometry(ElementState state, String property) {
            if ("local_x".equals(property)) return RuntimeValue.number(state.x);
            if ("local_y".equals(property)) return RuntimeValue.number(state.y);
            if ("width".equals(property)) return RuntimeValue.number(state.width);
            if ("height".equals(property)) return RuntimeValue.number(state.height);
            double[] world = baseWorld(state, elements);
            if ("world_x".equals(property)) return RuntimeValue.number(world[0]);
            if ("world_y".equals(property)) return RuntimeValue.number(world[1]);
            if ("world_center_x".equals(property)) return RuntimeValue.number(world[0] + state.width * 0.5D);
            if ("world_center_y".equals(property)) return RuntimeValue.number(world[1] + state.height * 0.5D);
            return null;
        }

        private double[] baseWorld(ElementState state, Map<String, ElementState> elements) {
            double x = state.x;
            double y = state.y;
            ElementState cursor = state;
            while (cursor.parentId != null) {
                cursor = elements.get(cursor.parentId);
                if (cursor == null) break;
                x += cursor.x;
                y += cursor.y;
            }
            RuntimeValue screenWidth = parent.get("screen.width");
            RuntimeValue screenHeight = parent.get("screen.height");
            RuntimeValue guiX = parent.get("gui.x");
            RuntimeValue guiY = parent.get("gui.y");
            RuntimeValue guiWidth = parent.get("gui.width");
            RuntimeValue guiHeight = parent.get("gui.height");
            double[] anchor = anchor(cursor == null ? state.anchor : cursor.anchor,
                    screenWidth.asNumber(), screenHeight.asNumber(), guiX.asNumber(), guiY.asNumber(),
                    guiWidth.asNumber(), guiHeight.asNumber());
            return new double[]{x + anchor[0], y + anchor[1]};
        }

        private double[] anchor(GuiDefinition.Anchor anchor, double screenWidth, double screenHeight,
                                double guiX, double guiY, double guiWidth, double guiHeight) {
            if (anchor == GuiDefinition.Anchor.GUI || anchor == GuiDefinition.Anchor.GUI_TOP_LEFT) {
                return new double[]{guiX, guiY};
            }
            if (anchor == GuiDefinition.Anchor.SCREEN || anchor == GuiDefinition.Anchor.SCREEN_TOP_LEFT
                    || anchor == GuiDefinition.Anchor.PARENT) return new double[]{0, 0};
            if (anchor == GuiDefinition.Anchor.SCREEN_CENTER) return new double[]{screenWidth / 2, screenHeight / 2};
            boolean gui = anchor.name().startsWith("GUI_");
            double left = gui ? guiX : 0.0D;
            double top = gui ? guiY : 0.0D;
            double width = gui ? guiWidth : screenWidth;
            double height = gui ? guiHeight : screenHeight;
            String name = anchor.name();
            double x = name.endsWith("_LEFT") ? left
                    : name.endsWith("_RIGHT") ? left + width : left + width * 0.5D;
            double y = name.contains("_TOP_") ? top
                    : name.contains("_BOTTOM_") ? top + height : top + height * 0.5D;
            return new double[]{x, y};
        }
    }
}
