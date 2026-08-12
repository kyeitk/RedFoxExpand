package redfoxexpand.reactive.animation;

import redfoxexpand.reactive.binding.ReactiveProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Transient animation output. Base layout is never mutated. */
public final class AnimationProperties {
    public static final AnimationProperties NONE = new AnimationProperties(
            Collections.<Contribution>emptyList());

    private final double translateX;
    private final double translateY;
    private final Double alpha;
    private final double scaleX;
    private final double scaleY;
    private final double rotationZ;
    private final List<Contribution> contributions;

    public AnimationProperties(double translateX, double translateY, Double alpha) {
        this(translateX, translateY, alpha, 1.0D, 1.0D);
    }

    public AnimationProperties(double translateX, double translateY, Double alpha,
                               double scaleX, double scaleY) {
        this(translateX, translateY, alpha, scaleX, scaleY, 0.0D);
    }

    public AnimationProperties(double translateX, double translateY, Double alpha,
                               double scaleX, double scaleY, double rotationZ) {
        List<Contribution> values = new ArrayList<Contribution>();
        values.add(new Contribution(ReactiveProperty.TRANSLATE_X, CompositionMode.ADD, translateX));
        values.add(new Contribution(ReactiveProperty.TRANSLATE_Y, CompositionMode.ADD, translateY));
        if (alpha != null) values.add(new Contribution(ReactiveProperty.ALPHA,
                CompositionMode.REPLACE, alpha.doubleValue()));
        values.add(new Contribution(ReactiveProperty.SCALE_X, CompositionMode.MULTIPLY, scaleX));
        values.add(new Contribution(ReactiveProperty.SCALE_Y, CompositionMode.MULTIPLY, scaleY));
        values.add(new Contribution(ReactiveProperty.ROTATION_Z, CompositionMode.ADD, rotationZ));
        this.contributions = Collections.unmodifiableList(values);
        this.translateX = apply(ReactiveProperty.TRANSLATE_X, 0.0D);
        this.translateY = apply(ReactiveProperty.TRANSLATE_Y, 0.0D);
        this.alpha = has(ReactiveProperty.ALPHA)
                ? Double.valueOf(apply(ReactiveProperty.ALPHA, 1.0D)) : null;
        this.scaleX = apply(ReactiveProperty.SCALE_X, 1.0D);
        this.scaleY = apply(ReactiveProperty.SCALE_Y, 1.0D);
        this.rotationZ = apply(ReactiveProperty.ROTATION_Z, 0.0D);
    }

    private AnimationProperties(List<Contribution> values) {
        this.contributions = Collections.unmodifiableList(new ArrayList<Contribution>(values));
        this.translateX = apply(ReactiveProperty.TRANSLATE_X, 0.0D);
        this.translateY = apply(ReactiveProperty.TRANSLATE_Y, 0.0D);
        this.alpha = has(ReactiveProperty.ALPHA)
                ? Double.valueOf(apply(ReactiveProperty.ALPHA, 1.0D)) : null;
        this.scaleX = apply(ReactiveProperty.SCALE_X, 1.0D);
        this.scaleY = apply(ReactiveProperty.SCALE_Y, 1.0D);
        this.rotationZ = apply(ReactiveProperty.ROTATION_Z, 0.0D);
    }

    public static AnimationProperties of(List<Contribution> values) {
        return values.isEmpty() ? NONE : new AnimationProperties(values);
    }

    public double getTranslateX() { return translateX; }
    public double getTranslateY() { return translateY; }
    public Double getAlpha() { return alpha; }
    public double getScaleX() { return scaleX; }
    public double getScaleY() { return scaleY; }
    public double getRotationZ() { return rotationZ; }
    public List<Contribution> getContributions() { return contributions; }

    public double apply(ReactiveProperty property, double base) {
        double result = base;
        for (Contribution contribution : contributions) {
            if (contribution.property != property) continue;
            if (contribution.mode == CompositionMode.REPLACE) result = contribution.value;
            else if (contribution.mode == CompositionMode.ADD) result += contribution.value;
            else result *= contribution.value;
        }
        return result;
    }

    private boolean has(ReactiveProperty property) {
        for (Contribution contribution : contributions) {
            if (contribution.property == property) return true;
        }
        return false;
    }

    public static final class Contribution {
        private final ReactiveProperty property;
        private final CompositionMode mode;
        private final double value;

        public Contribution(ReactiveProperty property, CompositionMode mode, double value) {
            this.property = property;
            this.mode = mode;
            this.value = value;
        }

        public ReactiveProperty getProperty() { return property; }
        public CompositionMode getMode() { return mode; }
        public double getValue() { return value; }
    }
}
