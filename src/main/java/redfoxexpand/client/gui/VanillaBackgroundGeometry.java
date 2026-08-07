package redfoxexpand.client.gui;

/**
 * Reconstructs the temporary GuiScreen dimensions needed by vanilla screens
 * that derive their background origin from (width - xSize) / 2.
 */
public final class VanillaBackgroundGeometry {

    private VanillaBackgroundGeometry() {
    }

    public static int screenWidthForOrigin(int guiLeft, int baseGuiWidth) {
        return guiLeft * 2 + baseGuiWidth;
    }

    public static int screenHeightForOrigin(int guiTop, int baseGuiHeight) {
        return guiTop * 2 + baseGuiHeight;
    }
}
