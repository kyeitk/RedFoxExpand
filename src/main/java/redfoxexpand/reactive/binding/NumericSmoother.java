package redfoxexpand.reactive.binding;

/** Render-time exponential smoothing for one numeric Binding target. */
public final class NumericSmoother {
    private boolean initialized;
    private double startValue;
    private double targetValue;
    private long startedAtMillis;
    private int smoothingMillis;

    public void update(double value, long nowMillis, int nextSmoothingMillis) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("smoothing value must be finite");
        }
        if (nextSmoothingMillis < 0) {
            throw new IllegalArgumentException("smoothingMillis must be non-negative");
        }
        if (!initialized || nextSmoothingMillis == 0) {
            initialized = true;
            startValue = value;
            targetValue = value;
            startedAtMillis = nowMillis;
            smoothingMillis = nextSmoothingMillis;
            return;
        }
        if (Double.compare(value, targetValue) == 0 && smoothingMillis == nextSmoothingMillis) return;
        startValue = sample(nowMillis);
        targetValue = value;
        startedAtMillis = nowMillis;
        smoothingMillis = nextSmoothingMillis;
    }

    public double sample(long nowMillis) {
        if (!initialized) throw new IllegalStateException("smoother has not been initialized");
        if (smoothingMillis == 0 || Double.compare(startValue, targetValue) == 0) return targetValue;
        long elapsed = Math.max(0L, nowMillis - startedAtMillis);
        if (elapsed >= (long) smoothingMillis * 8L) return targetValue;
        double factor = 1.0D - Math.exp(-(double) elapsed / (double) smoothingMillis);
        return startValue + (targetValue - startValue) * factor;
    }
}

