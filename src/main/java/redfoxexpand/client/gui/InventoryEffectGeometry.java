package redfoxexpand.client.gui;

/**
 * Geometry correction for {@code InventoryEffectRenderer}.
 *
 * <p>Minecraft 1.7.10 recalculates {@code guiLeft} from active effects in
 * {@code initGui} and moves
 * it when potion effects are visible. RedFoxExpand keeps the configured GUI
 * origin stable and moves the vanilla 140-pixel potion panel to the right of
 * the GUI instead.</p>
 */
public final class InventoryEffectGeometry {

    private static final int POTION_EFFECT_WIDTH = 140;
    private static final int POTION_EFFECT_GAP = 4;
    private static final int SCREEN_MARGIN = 4;

    private InventoryEffectGeometry() {
    }

    public static int stableGuiLeft(
            int screenWidth,
            int guiWidth,
            boolean configuredOriginReady,
            int configuredGuiLeft
    ) {
        return configuredOriginReady
                ? configuredGuiLeft
                : (screenWidth - guiWidth) / 2;
    }

    public static int rightPotionEffectsX(
            int guiLeft,
            int guiWidth,
            int screenWidth
    ) {
        int desiredX = guiLeft + guiWidth + POTION_EFFECT_GAP;
        int maxVisibleX = Math.max(0, screenWidth - POTION_EFFECT_WIDTH - SCREEN_MARGIN);
        return Math.max(0, Math.min(desiredX, maxVisibleX));
    }
}
