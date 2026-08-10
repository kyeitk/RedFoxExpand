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
    private final List<DefinitionRuntime> definitions = new ArrayList<DefinitionRuntime>();
    private final Map<DefinitionCandidate, DefinitionRuntime> byCandidate =
            new IdentityHashMap<DefinitionCandidate, DefinitionRuntime>();
    private RuntimeSnapshot previous;
    private RuntimeSnapshot current;

    public ReactiveScreenRuntime(List<DefinitionCandidate> candidates, RuntimeDiagnostics diagnostics) {
        for (DefinitionCandidate candidate : candidates) {
            if (candidate.apiVersion() != 3) continue;
            DefinitionRuntime runtime = new DefinitionRuntime(candidate, diagnostics);
            definitions.add(runtime);
            byCandidate.put(candidate, runtime);
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

    public FinalRenderProperties properties(SpriteOverlay sprite, long nowMillis) {
        DefinitionRuntime runtime = byCandidate.get(sprite.reactiveScope);
        ElementState state = runtime == null ? null : runtime.byId.get(sprite.elementId);
        if (state == null) return FinalRenderProperties.BASE;
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

    public RuntimeSnapshot currentSnapshot() {
        return current;
    }

    public void clear() {
        previous = null;
        current = null;
        animations.clear();
        for (DefinitionRuntime definition : definitions) definition.resetBindings();
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
            for (GuiDefinition.Sprite sprite : candidate.definition().sprites()) {
                if (sprite.id() == null) continue;
                ElementState state = new ElementState(scope + "\u0000" + sprite.id());
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
            for (Binding binding : definition.getBindings()) {
                ElementState target = byId.get(binding.getTarget());
                if (target == null) continue;
                try {
                    RuntimeValue value = binding.getExpression().evaluate(snapshot);
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
            behaviors.process(events, snapshot, new ActionSink() {
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
    }

    private static final class ElementState {
        final String scopedTarget;
        final Map<ReactiveProperty, NumericBindingState> numericBindings =
                new EnumMap<ReactiveProperty, NumericBindingState>(ReactiveProperty.class);
        Boolean boundVisible;
        Boolean overrideVisible;
        Double overrideAlpha;

        ElementState(String scopedTarget) {
            this.scopedTarget = scopedTarget;
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
}
