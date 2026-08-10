package redfoxexpand.reactive.animation;

/** Transient animation output. Base layout is never mutated. */
public final class AnimationProperties {
    public static final AnimationProperties NONE = new AnimationProperties(
            0.0D, 0.0D, null, 1.0D, 1.0D, 0.0D);

    private final double translateX;
    private final double translateY;
    private final Double alpha;
    private final double scaleX;
    private final double scaleY;
    private final double rotationZ;

    public AnimationProperties(double translateX, double translateY, Double alpha) {
        this(translateX, translateY, alpha, 1.0D, 1.0D);
    }

    public AnimationProperties(double translateX, double translateY, Double alpha,
                               double scaleX, double scaleY) {
        this(translateX, translateY, alpha, scaleX, scaleY, 0.0D);
    }

    public AnimationProperties(double translateX, double translateY, Double alpha,
                               double scaleX, double scaleY, double rotationZ) {
        this.translateX = translateX;
        this.translateY = translateY;
        this.alpha = alpha;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.rotationZ = rotationZ;
    }

    public double getTranslateX() { return translateX; }
    public double getTranslateY() { return translateY; }
    public Double getAlpha() { return alpha; }
    public double getScaleX() { return scaleX; }
    public double getScaleY() { return scaleY; }
    public double getRotationZ() { return rotationZ; }
}
