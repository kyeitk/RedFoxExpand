package redfoxexpand.reactive.expression;

import redfoxexpand.reactive.runtime.RuntimeContext;
import redfoxexpand.reactive.value.RuntimeValue;
import redfoxexpand.reactive.value.ValueType;

/** Immutable precompiled expression. */
public final class CompiledExpression {
    interface Node {
        RuntimeValue evaluate(RuntimeContext context);
        ValueType type();
        int depth();
    }

    private final String source;
    private final Node root;

    CompiledExpression(String source, Node root) {
        this.source = source;
        this.root = root;
    }

    public String getSource() {
        return source;
    }

    public ValueType getType() {
        return root.type();
    }

    public RuntimeValue evaluate(RuntimeContext context) {
        if (context == null) throw new ExpressionEvaluationException("runtime context is unavailable");
        return root.evaluate(context);
    }
}
