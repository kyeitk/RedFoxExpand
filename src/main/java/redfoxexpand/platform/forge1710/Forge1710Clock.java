package redfoxexpand.platform.forge1710;

import redfoxexpand.reactive.runtime.PlatformClock;

/** Monotonic clock shared by the 1.7.10 property and frame animation paths. */
public final class Forge1710Clock implements PlatformClock {
    public static final Forge1710Clock INSTANCE = new Forge1710Clock();
    private Forge1710Clock() { }
    @Override public long nowMillis() { return System.nanoTime() / 1000000L; }
}
