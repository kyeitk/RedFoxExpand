package redfoxexpand.reactive.binding;

import redfoxexpand.reactive.value.ValueType;

public enum ReactiveProperty {
    VISIBLE("visible", ValueType.BOOLEAN),
    ALPHA("alpha", ValueType.NUMBER),
    TRANSLATE_X("translate_x", ValueType.NUMBER),
    TRANSLATE_Y("translate_y", ValueType.NUMBER),
    SCALE_X("scale_x", ValueType.NUMBER),
    SCALE_Y("scale_y", ValueType.NUMBER),
    ROTATION_Z("rotation_z", ValueType.NUMBER);

    private final String schemaName;
    private final ValueType valueType;

    ReactiveProperty(String schemaName, ValueType valueType) {
        this.schemaName = schemaName;
        this.valueType = valueType;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public ValueType getValueType() {
        return valueType;
    }

    public static ReactiveProperty parse(String value) {
        for (ReactiveProperty property : values()) {
            if (property.schemaName.equals(value)) return property;
        }
        throw new IllegalArgumentException("unknown reactive property: " + value);
    }
}
