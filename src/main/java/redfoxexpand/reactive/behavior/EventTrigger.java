package redfoxexpand.reactive.behavior;

public final class EventTrigger {
    public enum Mode { COALESCE }

    private final String event;
    private final Double every;
    private final Mode mode;

    public EventTrigger(String event, Double every, Mode mode) {
        if (every != null && (every.doubleValue() <= 0.0D
                || Double.isNaN(every.doubleValue()) || Double.isInfinite(every.doubleValue()))) {
            throw new IllegalArgumentException("every must be a positive finite number");
        }
        this.event = event;
        this.every = every;
        this.mode = mode;
    }

    public String getEvent() { return event; }
    public Double getEvery() { return every; }
    public Mode getMode() { return mode; }
}
