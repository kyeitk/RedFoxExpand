package redfoxexpand.reactive.property;

/** Final transient values consumed by a platform renderer. */
public final class FinalRenderProperties {
    public static final FinalRenderProperties BASE = new FinalRenderProperties(
            true, 1.0D, 0.0D, 0.0D, 1.0D, 1.0D, 0.0D);

    private final boolean visible;
    private final double alpha;
    private final double translateX;
    private final double translateY;
    private final double scaleX;
    private final double scaleY;
    private final double rotationZ;

    public FinalRenderProperties(boolean visible, double alpha, double translateX, double translateY) {
        this(visible, alpha, translateX, translateY, 1.0D, 1.0D);
    }

    public FinalRenderProperties(boolean visible, double alpha, double translateX, double translateY,
                                 double scaleX, double scaleY) {
        this(visible, alpha, translateX, translateY, scaleX, scaleY, 0.0D);
    }

    public FinalRenderProperties(boolean visible, double alpha, double translateX, double translateY,
                                 double scaleX, double scaleY, double rotationZ) {
        this.visible = visible;
        this.alpha = alpha;
        this.translateX = translateX;
        this.translateY = translateY;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.rotationZ = rotationZ;
    }

    public boolean isVisible() { return visible; }
    public double getAlpha() { return alpha; }
    public double getTranslateX() { return translateX; }
    public double getTranslateY() { return translateY; }
    public double getScaleX() { return scaleX; }
    public double getScaleY() { return scaleY; }
    public double getRotationZ() { return rotationZ; }
}
