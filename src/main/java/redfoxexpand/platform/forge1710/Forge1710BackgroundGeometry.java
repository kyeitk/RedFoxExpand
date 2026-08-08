package redfoxexpand.platform.forge1710;

/**
 * 1.7.10 background adapter which keeps the real screen dimensions visible
 * while translating the vanilla 256-pixel GUI texture to the configured
 * origin.
 */
public final class Forge1710BackgroundGeometry {

    private Forge1710BackgroundGeometry() {
    }

    public static int centeredLeft(int screenWidth, int guiWidth) {
        return (screenWidth - guiWidth) / 2;
    }

    public static int centeredTop(int screenHeight, int guiHeight) {
        return (screenHeight - guiHeight) / 2;
    }

    public static int translation(int desiredOrigin, int centeredOrigin) {
        return desiredOrigin - centeredOrigin;
    }
}
