package redfoxexpand.platform.forge189;

import redfoxexpand.RedFoxExpand;
import redfoxexpand.reactive.runtime.RuntimeDiagnostics;

import java.util.HashSet;
import java.util.Set;

/** Per-screen, bounded diagnostic sink for failures that can only occur during evaluation. */
public final class Forge189RuntimeDiagnostics implements RuntimeDiagnostics {
    private static final int MAX_UNIQUE_WARNINGS = 64;
    private final Set<String> warnedKeys = new HashSet<String>();
    private boolean overflowReported;

    @Override
    public void warning(String key, String message, Throwable error) {
        if (key == null || warnedKeys.contains(key)) return;
        if (warnedKeys.size() >= MAX_UNIQUE_WARNINGS) {
            if (!overflowReported) {
                overflowReported = true;
                RedFoxExpand.LOGGER.warn(
                        "Schema v3 runtime warning limit reached for this GUI; further unique warnings are suppressed");
            }
            return;
        }
        warnedKeys.add(key);
        RedFoxExpand.LOGGER.warn("{} [{}]", message, key, error);
    }
}
