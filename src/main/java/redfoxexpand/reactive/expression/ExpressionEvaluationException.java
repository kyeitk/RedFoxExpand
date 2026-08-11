package redfoxexpand.reactive.expression;

/** Runtime expression failure. Callers must use a safe property/action fallback. */
public final class ExpressionEvaluationException extends RuntimeException {
    public ExpressionEvaluationException(String message) {
        super(message);
    }
}

