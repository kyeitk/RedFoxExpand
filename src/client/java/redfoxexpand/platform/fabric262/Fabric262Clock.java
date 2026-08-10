package redfoxexpand.platform.fabric262;

import redfoxexpand.reactive.runtime.PlatformClock;

/** Monotonic clock shared by texture and property animation runtimes. */
public final class Fabric262Clock implements PlatformClock {
    public static final Fabric262Clock INSTANCE = new Fabric262Clock();

    private Fabric262Clock() {
    }

    @Override
    public long nowMillis() {
        return System.nanoTime() / 1_000_000L;
    }
}
