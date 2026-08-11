package redfoxexpand.reactive.runtime;

/** Platform-owned, rate-limited diagnostic sink for runtime-only failures. */
public interface RuntimeDiagnostics {
    void warning(String key, String message, Throwable error);
}

