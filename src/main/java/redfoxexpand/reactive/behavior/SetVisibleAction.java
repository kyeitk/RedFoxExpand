package redfoxexpand.reactive.behavior;

public final class SetVisibleAction implements Action {
    private final String target;
    private final boolean value;

    public SetVisibleAction(String target, boolean value) {
        this.target = target;
        this.value = value;
    }

    public Type getType() { return Type.SET_VISIBLE; }
    public String getTarget() { return target; }
    public boolean getValue() { return value; }
}
