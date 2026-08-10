package redfoxexpand.platform.fabric262;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import redfoxexpand.core.GuiContext;
import redfoxexpand.core.GuiDefinition;
import redfoxexpand.reactive.runtime.RuntimeSnapshot;

/** Minecraft 26.2-only adapter from LocalPlayer/Screen state to the pure runtime snapshot. */
public final class Fabric262RuntimeStateProvider {
    private Fabric262RuntimeStateProvider() {
    }

    public static RuntimeSnapshot snapshot(Minecraft client, GuiContext context,
                                           GuiDefinition.Geometry geometry, int leftDelta, int topDelta) {
        LocalPlayer player = client.player;
        if (player == null || context == null || geometry == null) return null;
        double mouseX = finiteMouseCoordinate(client.mouseHandler.getScaledXPos(client.getWindow()));
        double mouseY = finiteMouseCoordinate(client.mouseHandler.getScaledYPos(client.getWindow()));
        double guiX = context.leftPos() + leftDelta;
        double guiY = context.topPos() + topDelta;
        return RuntimeSnapshot.builder()
                .number("player.health", player.getHealth())
                .number("player.max_health", player.getMaxHealth())
                .bool("player.is_burning", player.isOnFire())
                .bool("player.is_sneaking", player.isShiftKeyDown())
                .bool("player.is_sprinting", player.isSprinting())
                .number("player.armor", player.getArmorValue())
                .number("player.food", player.getFoodData().getFoodLevel())
                .number("player.air", player.getAirSupply())
                .number("player.level", player.experienceLevel)
                .number("player.experience", player.experienceProgress)
                .number("screen.width", context.screenWidth())
                .number("screen.height", context.screenHeight())
                .number("gui.x", guiX)
                .number("gui.y", guiY)
                .number("gui.width", context.imageWidth() + geometry.widthOffset())
                .number("gui.height", context.imageHeight() + geometry.heightOffset())
                .number("mouse.x", mouseX)
                .number("mouse.y", mouseY)
                .number("mouse.gui_x", guiRelativeCoordinate(mouseX, guiX))
                .number("mouse.gui_y", guiRelativeCoordinate(mouseY, guiY))
                .bool("mouse.left_down", client.mouseHandler.isLeftPressed())
                .bool("mouse.right_down", client.mouseHandler.isRightPressed())
                .build();
    }

    /** A transient zero-sized window or invalid native cursor value must not poison the strict Core snapshot. */
    static double finiteMouseCoordinate(double value) {
        return Double.isFinite(value) ? value : 0.0D;
    }

    static double guiRelativeCoordinate(double mouseCoordinate, double guiOrigin) {
        return mouseCoordinate - guiOrigin;
    }
}
