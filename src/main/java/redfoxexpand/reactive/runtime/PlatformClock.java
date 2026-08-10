package redfoxexpand.reactive.runtime;

/** Monotonic millisecond clock supplied by each Minecraft platform adapter. */
public interface PlatformClock {
    long nowMillis();
}
