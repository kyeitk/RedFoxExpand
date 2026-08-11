package redfoxexpand.reactive.runtime;

import redfoxexpand.reactive.value.ValueType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Stable Schema v3 variable names and their compile-time types. */
public final class RuntimeVariables {
    private static final Map<String, ValueType> STATE_TYPES;
    private static final Map<String, ValueType> EVENT_TYPES;

    static {
        Map<String, ValueType> state = new LinkedHashMap<String, ValueType>();
        state.put("player.health", ValueType.NUMBER);
        state.put("player.max_health", ValueType.NUMBER);
        state.put("player.is_burning", ValueType.BOOLEAN);
        state.put("player.is_sneaking", ValueType.BOOLEAN);
        state.put("player.is_sprinting", ValueType.BOOLEAN);
        state.put("player.armor", ValueType.NUMBER);
        state.put("player.food", ValueType.NUMBER);
        state.put("player.air", ValueType.NUMBER);
        state.put("player.level", ValueType.NUMBER);
        state.put("player.experience", ValueType.NUMBER);
        state.put("screen.width", ValueType.NUMBER);
        state.put("screen.height", ValueType.NUMBER);
        state.put("gui.x", ValueType.NUMBER);
        state.put("gui.y", ValueType.NUMBER);
        state.put("gui.width", ValueType.NUMBER);
        state.put("gui.height", ValueType.NUMBER);
        state.put("mouse.x", ValueType.NUMBER);
        state.put("mouse.y", ValueType.NUMBER);
        state.put("mouse.gui_x", ValueType.NUMBER);
        state.put("mouse.gui_y", ValueType.NUMBER);
        state.put("mouse.left_down", ValueType.BOOLEAN);
        state.put("mouse.right_down", ValueType.BOOLEAN);
        STATE_TYPES = Collections.unmodifiableMap(state);

        Map<String, ValueType> event = new LinkedHashMap<String, ValueType>(state);
        event.put("event.old", ValueType.NUMBER);
        event.put("event.new", ValueType.NUMBER);
        event.put("event.delta", ValueType.NUMBER);
        EVENT_TYPES = Collections.unmodifiableMap(event);
    }

    private RuntimeVariables() {
    }

    public static Map<String, ValueType> stateTypes() {
        return STATE_TYPES;
    }

    public static Map<String, ValueType> eventTypes() {
        return EVENT_TYPES;
    }
}

