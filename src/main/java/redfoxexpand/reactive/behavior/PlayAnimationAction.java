package redfoxexpand.reactive.behavior;

public final class PlayAnimationAction implements Action {
    private final String target;
    private final String animation;
    private final boolean restart;

    public PlayAnimationAction(String target, String animation, boolean restart) {
        this.target = target;
        this.animation = animation;
        this.restart = restart;
    }

    public Type getType() { return Type.PLAY_ANIMATION; }
    public String getTarget() { return target; }
    public String getAnimation() { return animation; }
    public boolean isRestart() { return restart; }
}

