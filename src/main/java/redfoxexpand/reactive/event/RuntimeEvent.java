package redfoxexpand.reactive.event;

import redfoxexpand.reactive.runtime.RuntimeContext;
import redfoxexpand.reactive.value.RuntimeValue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** One tick-scoped event with an immutable payload. */
public final class RuntimeEvent {
    public static final String HEALTH_DECREASED = "player.health.decreased";
    public static final String HEALTH_INCREASED = "player.health.increased";
    public static final String STARTED_BURNING = "player.started_burning";
    public static final String STOPPED_BURNING = "player.stopped_burning";
    public static final String SCREEN_OPENED = "screen.opened";

    private final String id;
    private final Map<String, RuntimeValue> payload;

    private RuntimeEvent(String id, Map<String, RuntimeValue> payload) {
        this.id = id;
        this.payload = Collections.unmodifiableMap(new LinkedHashMap<String, RuntimeValue>(payload));
    }

    public static RuntimeEvent health(String id, double oldValue, double newValue) {
        Map<String, RuntimeValue> payload = new LinkedHashMap<String, RuntimeValue>();
        payload.put("event.old", RuntimeValue.number(oldValue));
        payload.put("event.new", RuntimeValue.number(newValue));
        payload.put("event.delta", RuntimeValue.number(Math.abs(newValue - oldValue)));
        return new RuntimeEvent(id, payload);
    }

    public static RuntimeEvent state(String id) {
        return new RuntimeEvent(id, Collections.<String, RuntimeValue>emptyMap());
    }

    public String getId() {
        return id;
    }

    public RuntimeValue get(String name) {
        return payload.get(name);
    }

    public double getDelta() {
        RuntimeValue value = payload.get("event.delta");
        return value == null ? 0.0D : value.asNumber();
    }

    public RuntimeContext context(final RuntimeContext base) {
        final RuntimeEvent event = this;
        return new RuntimeContext() {
            @Override
            public RuntimeValue get(String name) {
                RuntimeValue value = event.get(name);
                return value != null ? value : base.get(name);
            }
        };
    }
}

