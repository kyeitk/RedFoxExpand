package redfoxexpand.reactive.animation;

public final class PropertyKeyframe {
    private final long timeMillis;
    private final double value;

    public PropertyKeyframe(long timeMillis, double value) {
        if (timeMillis < 0L) throw new IllegalArgumentException("keyframe time must be non-negative");
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("keyframe value must be finite");
        }
        this.timeMillis = timeMillis;
        this.value = value;
    }

    public long getTimeMillis() { return timeMillis; }
    public double getValue() { return value; }
}
