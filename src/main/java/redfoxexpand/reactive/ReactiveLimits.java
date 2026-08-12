package redfoxexpand.reactive;

/** Java 8-compatible safety budgets for untrusted Schema v3 input. */
public final class ReactiveLimits {
    public static final int MAX_BINDINGS_PER_DEFINITION = 128;
    public static final int MAX_BEHAVIORS_PER_DEFINITION = 128;
    public static final int MAX_ANIMATIONS_PER_DEFINITION = 64;
    public static final int MAX_TRACKS_PER_ANIMATION = 16;
    public static final int MAX_KEYFRAMES_PER_TRACK = 128;
    public static final int MAX_ACTIONS_PER_BEHAVIOR = 32;
    public static final int MAX_ACTIVE_ANIMATIONS_PER_SCREEN = 32;
    public static final int MAX_BINDING_SMOOTHING_MS = 600000;
    public static final int MAX_CONSTANTS_PER_DEFINITION = 128;
    public static final int MAX_DERIVED_VALUES_PER_DEFINITION = 128;
    public static final int MAX_EXPRESSION_CHARS = 1024;
    public static final int MAX_EXPRESSION_TOKENS = 256;
    public static final int MAX_EXPRESSION_DEPTH = 32;
    public static final int MAX_FUNCTION_ARGUMENTS = 3;

    private ReactiveLimits() {
    }
}

