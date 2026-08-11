package redfoxexpand.reactive.property;

import redfoxexpand.reactive.animation.AnimationProperties;

/** Base -> Binding -> Animation -> Runtime Override composition without mutating layout. */
public final class PropertyPipeline {
    private PropertyPipeline() {
    }

    public static FinalRenderProperties resolve(Boolean boundVisible, Double boundAlpha,
                                                Double boundTranslateX, Double boundTranslateY,
                                                Double boundScaleX, Double boundScaleY,
                                                Double boundRotationZ,
                                                AnimationProperties animation,
                                                Boolean overrideVisible, Double overrideAlpha) {
        boolean visible = boundVisible == null || boundVisible.booleanValue();
        double alpha = boundAlpha == null ? 1.0D : boundAlpha.doubleValue();
        double translateX = boundTranslateX == null ? 0.0D : boundTranslateX.doubleValue();
        double translateY = boundTranslateY == null ? 0.0D : boundTranslateY.doubleValue();
        double scaleX = boundScaleX == null ? 1.0D : boundScaleX.doubleValue();
        double scaleY = boundScaleY == null ? 1.0D : boundScaleY.doubleValue();
        double rotationZ = boundRotationZ == null ? 0.0D : boundRotationZ.doubleValue();
        if (animation != null) {
            translateX += animation.getTranslateX();
            translateY += animation.getTranslateY();
            if (animation.getAlpha() != null) alpha = animation.getAlpha().doubleValue();
            scaleX *= animation.getScaleX();
            scaleY *= animation.getScaleY();
            rotationZ += animation.getRotationZ();
        }
        if (overrideVisible != null) visible = overrideVisible.booleanValue();
        if (overrideAlpha != null) alpha = overrideAlpha.doubleValue();
        if (Double.isNaN(alpha) || Double.isInfinite(alpha)) alpha = 1.0D;
        alpha = Math.max(0.0D, Math.min(1.0D, alpha));
        scaleX = safeScale(scaleX);
        scaleY = safeScale(scaleY);
        rotationZ = safeRotation(rotationZ);
        return new FinalRenderProperties(visible, alpha, translateX, translateY,
                scaleX, scaleY, rotationZ);
    }

    /** Compatibility overload for callers that predate rotation bindings. */
    public static FinalRenderProperties resolve(Boolean boundVisible, Double boundAlpha,
                                                Double boundTranslateX, Double boundTranslateY,
                                                Double boundScaleX, Double boundScaleY,
                                                AnimationProperties animation,
                                                Boolean overrideVisible, Double overrideAlpha) {
        return resolve(boundVisible, boundAlpha, boundTranslateX, boundTranslateY,
                boundScaleX, boundScaleY, null, animation, overrideVisible, overrideAlpha);
    }

    /** Compatibility overload for callers that predate scale bindings. */
    public static FinalRenderProperties resolve(Boolean boundVisible, Double boundAlpha,
                                                Double boundTranslateX, Double boundTranslateY,
                                                AnimationProperties animation,
                                                Boolean overrideVisible, Double overrideAlpha) {
        return resolve(boundVisible, boundAlpha, boundTranslateX, boundTranslateY,
                null, null, null, animation, overrideVisible, overrideAlpha);
    }

    private static double safeScale(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return 1.0D;
        return Math.max(0.0D, Math.min(8.0D, value));
    }

    private static double safeRotation(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return 0.0D;
        return Math.max(-360.0D, Math.min(360.0D, value));
    }
}

