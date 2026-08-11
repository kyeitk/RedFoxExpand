package redfoxexpand.reactive.value;

/** Closed value type used by the cross-version expression/runtime core. */
public final class RuntimeValue {
    private static final RuntimeValue TRUE = new RuntimeValue(ValueType.BOOLEAN, 0.0D, true, null);
    private static final RuntimeValue FALSE = new RuntimeValue(ValueType.BOOLEAN, 0.0D, false, null);

    private final ValueType type;
    private final double number;
    private final boolean bool;
    private final String string;

    private RuntimeValue(ValueType type, double number, boolean bool, String string) {
        this.type = type;
        this.number = number;
        this.bool = bool;
        this.string = string;
    }

    public static RuntimeValue number(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("runtime number must be finite");
        }
        return new RuntimeValue(ValueType.NUMBER, value, false, null);
    }

    public static RuntimeValue bool(boolean value) {
        return value ? TRUE : FALSE;
    }

    public static RuntimeValue string(String value) {
        if (value == null) throw new IllegalArgumentException("runtime string must not be null");
        return new RuntimeValue(ValueType.STRING, 0.0D, false, value);
    }

    public ValueType getType() {
        return type;
    }

    public double asNumber() {
        require(ValueType.NUMBER);
        return number;
    }

    public boolean asBoolean() {
        require(ValueType.BOOLEAN);
        return bool;
    }

    public String asString() {
        require(ValueType.STRING);
        return string;
    }

    private void require(ValueType expected) {
        if (type != expected) {
            throw new IllegalStateException("expected " + expected + " but found " + type);
        }
    }
}

