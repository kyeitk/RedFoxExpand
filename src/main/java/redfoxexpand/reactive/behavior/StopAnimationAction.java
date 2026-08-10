package redfoxexpand.reactive.behavior;

public final class StopAnimationAction implements Action {
    private final String target;
    private final String animation;

    public StopAnimationAction(String target, String animation) {
        this.target = target;
        this.animation = animation;
    }

    public Type getType() { return Type.STOP_ANIMATION; }
    public String getTarget() { return target; }
    public String getAnimation() { return animation; }
}
