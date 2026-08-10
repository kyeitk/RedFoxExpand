package redfoxexpand.client.gui;

import org.slf4j.Logger;
import redfoxexpand.reactive.runtime.PlatformClock;
import redfoxexpand.reactive.runtime.RuntimeDiagnostics;

import java.util.HashMap;
import java.util.Map;

/** Prevents a broken runtime value from logging every render/tick. */
final class RateLimitedRuntimeDiagnostics implements RuntimeDiagnostics {
    private static final long INTERVAL_MILLIS = 5_000L;
    private final Logger logger;
    private final PlatformClock clock;
    private final Map<String, Long> lastWarnings = new HashMap<>();

    RateLimitedRuntimeDiagnostics(Logger logger, PlatformClock clock) {
        this.logger = logger;
        this.clock = clock;
    }

    @Override
    public void warning(String key, String message, Throwable error) {
        long now = clock.nowMillis();
        Long previous = lastWarnings.get(key);
        if (previous != null && now - previous < INTERVAL_MILLIS) return;
        lastWarnings.put(key, now);
        logger.warn("{}: {}", message, error.getMessage());
    }
}
