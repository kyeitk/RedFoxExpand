package redfoxexpand.reactive;

import redfoxexpand.reactive.animation.PropertyAnimation;
import redfoxexpand.reactive.behavior.BehaviorRule;
import redfoxexpand.reactive.binding.Binding;
import redfoxexpand.reactive.expression.DerivedValue;
import redfoxexpand.reactive.value.RuntimeValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable reactive extension attached to one strict v2-compatible Definition. */
public final class ReactiveDefinition {
    public static final ReactiveDefinition EMPTY = new ReactiveDefinition(
            Collections.<Binding>emptyList(), Collections.<String, PropertyAnimation>emptyMap(),
            Collections.<BehaviorRule>emptyList(), Collections.<String, RuntimeValue>emptyMap(),
            Collections.<DerivedValue>emptyList());

    private final List<Binding> bindings;
    private final Map<String, PropertyAnimation> animations;
    private final List<BehaviorRule> behaviors;
    private final Map<String, RuntimeValue> constants;
    private final List<DerivedValue> values;

    public ReactiveDefinition(List<Binding> bindings, Map<String, PropertyAnimation> animations,
                              List<BehaviorRule> behaviors) {
        this(bindings, animations, behaviors, Collections.<String, RuntimeValue>emptyMap(),
                Collections.<DerivedValue>emptyList());
    }

    public ReactiveDefinition(List<Binding> bindings, Map<String, PropertyAnimation> animations,
                              List<BehaviorRule> behaviors, Map<String, RuntimeValue> constants,
                              List<DerivedValue> values) {
        this.bindings = Collections.unmodifiableList(new ArrayList<Binding>(bindings));
        this.animations = Collections.unmodifiableMap(new LinkedHashMap<String, PropertyAnimation>(animations));
        this.behaviors = Collections.unmodifiableList(new ArrayList<BehaviorRule>(behaviors));
        this.constants = Collections.unmodifiableMap(new LinkedHashMap<String, RuntimeValue>(constants));
        this.values = Collections.unmodifiableList(new ArrayList<DerivedValue>(values));
    }

    public List<Binding> getBindings() { return bindings; }
    public Map<String, PropertyAnimation> getAnimations() { return animations; }
    public List<BehaviorRule> getBehaviors() { return behaviors; }
    public Map<String, RuntimeValue> getConstants() { return constants; }
    public List<DerivedValue> getValues() { return values; }

    public boolean isEmpty() {
        return bindings.isEmpty() && animations.isEmpty() && behaviors.isEmpty()
                && constants.isEmpty() && values.isEmpty();
    }
}

