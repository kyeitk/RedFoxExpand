package redfoxexpand.reactive.behavior;

public interface Action {
    Type getType();

    enum Type {
        PLAY_ANIMATION,
        STOP_ANIMATION,
        SET_VISIBLE,
        SET_ALPHA
    }
}
