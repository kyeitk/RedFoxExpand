package redfoxexpand.reactive.expression;

/** Definition-local, read-only Schema v3.1 value evaluated in declaration order. */
public final class DerivedValue {
    private final String name;
    private final CompiledExpression expression;

    public DerivedValue(String name, CompiledExpression expression) {
        this.name = name;
        this.expression = expression;
    }

    public String getName() { return name; }
    public CompiledExpression getExpression() { return expression; }
}

