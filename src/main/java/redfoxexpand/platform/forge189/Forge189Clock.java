package redfoxexpand.platform.forge189;

import redfoxexpand.reactive.runtime.PlatformClock;

/** Monotonic clock shared by the 1.8.9 property and frame animation paths. */
public final class Forge189Clock implements PlatformClock {
    public static final Forge189Clock INSTANCE = new Forge189Clock();
    private Forge189Clock() { }
    @Override public long nowMillis() { return System.nanoTime() / 1000000L; }
}
