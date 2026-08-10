package redfoxexpand.client.gui;

public final class FontRenderContext {

    private static final ThreadLocal<State> CURRENT = new ThreadLocal<State>();

    private FontRenderContext() {
    }

    public static void begin(ResolvedGuiModifier modifier) {
        if (modifier == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(new State(modifier));
        }
    }

    public static void end() {
        CURRENT.remove();
    }

    public static AdjustedText adjust(int x, int y, int color) {
        State state = CURRENT.get();
        if (state == null) {
            return null;
        }

        int call = state.callIndex++;
        if (call == 0) {
            return new AdjustedText(
                    x + state.modifier.titleXOffset,
                    y + state.modifier.titleYOffset,
                    state.modifier.titleColor == null ? color : state.modifier.titleColor.intValue()
                    , state.modifier.titleHidden
            );
        }
        if (call == 1) {
            return new AdjustedText(
                    x + state.modifier.labelXOffset,
                    y + state.modifier.labelYOffset,
                    state.modifier.labelColor == null ? color : state.modifier.labelColor.intValue()
                    , state.modifier.labelHidden
            );
        }
        return null;
    }

    private static final class State {
        private final ResolvedGuiModifier modifier;
        private int callIndex;

        private State(ResolvedGuiModifier modifier) {
            this.modifier = modifier;
        }
    }

    public static final class AdjustedText {
        public final int x;
        public final int y;
        public final int color;
        public final boolean hidden;

        private AdjustedText(int x, int y, int color, boolean hidden) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.hidden = hidden;
        }
    }
}
