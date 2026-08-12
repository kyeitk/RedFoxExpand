package redfoxexpand.reactive.animation;

import redfoxexpand.reactive.binding.ReactiveProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One numeric property track with bounded deterministic interpolation. */
public final class PropertyTrack {
    private final ReactiveProperty property;
    private final Interpolation interpolation;
    private final CompositionMode composition;
    private final List<PropertyKeyframe> keyframes;

    public PropertyTrack(ReactiveProperty property, Interpolation interpolation,
                         List<PropertyKeyframe> keyframes) {
        this(property, interpolation, CompositionMode.defaultFor(property), keyframes);
    }

    public PropertyTrack(ReactiveProperty property, Interpolation interpolation,
                         CompositionMode composition, List<PropertyKeyframe> keyframes) {
        if (property == ReactiveProperty.VISIBLE) {
            throw new IllegalArgumentException("visible is not an animation track property");
        }
        if (keyframes == null || keyframes.isEmpty()) {
            throw new IllegalArgumentException("animation track requires keyframes");
        }
        this.property = property;
        this.interpolation = interpolation;
        this.composition = composition;
        this.keyframes = Collections.unmodifiableList(new ArrayList<PropertyKeyframe>(keyframes));
    }

    public ReactiveProperty getProperty() { return property; }
    public Interpolation getInterpolation() { return interpolation; }
    public CompositionMode getComposition() { return composition; }
    public List<PropertyKeyframe> getKeyframes() { return keyframes; }

    public double sample(long timeMillis) {
        PropertyKeyframe first = keyframes.get(0);
        if (timeMillis <= first.getTimeMillis()) return first.getValue();
        for (int index = 1; index < keyframes.size(); index++) {
            PropertyKeyframe right = keyframes.get(index);
            if (timeMillis <= right.getTimeMillis()) {
                PropertyKeyframe left = keyframes.get(index - 1);
                long span = right.getTimeMillis() - left.getTimeMillis();
                if (span <= 0L) return right.getValue();
                double progress = (double) (timeMillis - left.getTimeMillis()) / (double) span;
                if (interpolation == Interpolation.SMOOTHSTEP) {
                    progress = progress * progress * (3.0D - 2.0D * progress);
                }
                return left.getValue() + (right.getValue() - left.getValue()) * progress;
            }
        }
        return keyframes.get(keyframes.size() - 1).getValue();
    }
}
