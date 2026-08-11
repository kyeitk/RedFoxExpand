package redfoxexpand.reactive.binding;

import redfoxexpand.reactive.expression.CompiledExpression;

/** A precompiled continuous property binding. */
public final class Binding {
    private final String target;
    private final ReactiveProperty property;
    private final CompiledExpression expression;
    private final int smoothingMillis;

    public Binding(String target, ReactiveProperty property, CompiledExpression expression) {
        this(target, property, expression, 0);
    }

    public Binding(String target, ReactiveProperty property, CompiledExpression expression,
                   int smoothingMillis) {
        if (smoothingMillis < 0) throw new IllegalArgumentException("smoothingMillis must be non-negative");
        this.target = target;
        this.property = property;
        this.expression = expression;
        this.smoothingMillis = smoothingMillis;
    }

    public String getTarget() { return target; }
    public ReactiveProperty getProperty() { return property; }
    public CompiledExpression getExpression() { return expression; }
    public int getSmoothingMillis() { return smoothingMillis; }
}

