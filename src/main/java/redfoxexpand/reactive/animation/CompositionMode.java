package redfoxexpand.reactive.animation;

import redfoxexpand.reactive.binding.ReactiveProperty;

/** Explicit Schema v3.1 contribution mode for one numeric animation track. */
public enum CompositionMode {
    REPLACE,
    ADD,
    MULTIPLY;

    public static CompositionMode defaultFor(ReactiveProperty property) {
        if (property == ReactiveProperty.SCALE_X || property == ReactiveProperty.SCALE_Y) {
            return MULTIPLY;
        }
        if (property == ReactiveProperty.ALPHA) return REPLACE;
        return ADD;
    }

    public static CompositionMode parse(String value) {
        if ("replace".equals(value)) return REPLACE;
        if ("add".equals(value)) return ADD;
        if ("multiply".equals(value)) return MULTIPLY;
        throw new IllegalArgumentException("unknown animation composition mode: " + value);
    }
}
