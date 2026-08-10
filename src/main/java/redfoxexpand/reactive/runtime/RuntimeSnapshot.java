package redfoxexpand.reactive.runtime;

import redfoxexpand.reactive.value.RuntimeValue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable state snapshot. Platform adapters build one snapshot per client tick. */
public final class RuntimeSnapshot implements RuntimeContext {
    private final Map<String, RuntimeValue> values;

    private RuntimeSnapshot(Map<String, RuntimeValue> values) {
        this.values = Collections.unmodifiableMap(new LinkedHashMap<String, RuntimeValue>(values));
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public RuntimeValue get(String name) {
        return values.get(name);
    }

    public double number(String name) {
        RuntimeValue value = values.get(name);
        if (value == null) throw new IllegalStateException("missing runtime number: " + name);
        return value.asNumber();
    }

    public boolean bool(String name) {
        RuntimeValue value = values.get(name);
        if (value == null) throw new IllegalStateException("missing runtime boolean: " + name);
        return value.asBoolean();
    }

    public static final class Builder {
        private final Map<String, RuntimeValue> values = new LinkedHashMap<String, RuntimeValue>();

        public Builder number(String name, double value) {
            requireType(name, redfoxexpand.reactive.value.ValueType.NUMBER);
            values.put(name, RuntimeValue.number(value));
            return this;
        }

        public Builder bool(String name, boolean value) {
            requireType(name, redfoxexpand.reactive.value.ValueType.BOOLEAN);
            values.put(name, RuntimeValue.bool(value));
            return this;
        }

        public Builder string(String name, String value) {
            requireType(name, redfoxexpand.reactive.value.ValueType.STRING);
            values.put(name, RuntimeValue.string(value));
            return this;
        }

        public RuntimeSnapshot build() {
            for (String name : RuntimeVariables.stateTypes().keySet()) {
                if (!values.containsKey(name)) {
                    throw new IllegalStateException("missing runtime variable: " + name);
                }
            }
            return new RuntimeSnapshot(values);
        }

        private static void requireType(String name, redfoxexpand.reactive.value.ValueType actual) {
            redfoxexpand.reactive.value.ValueType expected = RuntimeVariables.stateTypes().get(name);
            if (expected == null) throw new IllegalArgumentException("unknown runtime variable: " + name);
            if (expected != actual) throw new IllegalArgumentException("wrong type for runtime variable: " + name);
        }
    }
}
