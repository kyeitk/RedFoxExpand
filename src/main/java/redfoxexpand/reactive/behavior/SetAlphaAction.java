package redfoxexpand.reactive.behavior;

public final class SetAlphaAction implements Action {
    private final String target;
    private final double value;

    public SetAlphaAction(String target, double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("alpha action value must be finite");
        }
        this.target = target;
        this.value = value;
    }

    public Type getType() { return Type.SET_ALPHA; }
    public String getTarget() { return target; }
    public double getValue() { return value; }
}
