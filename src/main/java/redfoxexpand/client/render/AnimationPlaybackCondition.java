package redfoxexpand.client.render;

/** Extension point for cached GUI animation playback conditions. */
public interface AnimationPlaybackCondition {

    boolean shouldPlay();
}
