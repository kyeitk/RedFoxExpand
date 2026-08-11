package redfoxexpand.platform.forge1710;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import org.lwjgl.input.Mouse;
import redfoxexpand.reactive.runtime.RuntimeSnapshot;

/** Forge 1.7.10-only adapter from live client state to the pure Schema v3 snapshot. */
public final class Forge1710RuntimeStateProvider {
    private Forge1710RuntimeStateProvider() { }

    public static RuntimeSnapshot snapshot(Minecraft client, int screenWidth, int screenHeight,
                                           int guiX, int guiY, int guiWidth, int guiHeight) {
        EntityClientPlayerMP player = client.thePlayer;
        if (player == null) return null;
        double mouseX = scaledMouseX(client, screenWidth);
        double mouseY = scaledMouseY(client, screenHeight);
        return RuntimeSnapshot.builder()
                .number("player.health", player.getHealth())
                .number("player.max_health", player.getMaxHealth())
                .bool("player.is_burning", player.isBurning())
                .bool("player.is_sneaking", player.isSneaking())
                .bool("player.is_sprinting", player.isSprinting())
                .number("player.armor", player.getTotalArmorValue())
                .number("player.food", player.getFoodStats().getFoodLevel())
                .number("player.air", player.getAir())
                .number("player.level", player.experienceLevel)
                .number("player.experience", player.experience)
                .number("screen.width", screenWidth)
                .number("screen.height", screenHeight)
                .number("gui.x", guiX)
                .number("gui.y", guiY)
                .number("gui.width", guiWidth)
                .number("gui.height", guiHeight)
                .number("mouse.x", mouseX)
                .number("mouse.y", mouseY)
                .number("mouse.gui_x", mouseX - guiX)
                .number("mouse.gui_y", mouseY - guiY)
                .bool("mouse.left_down", Mouse.isButtonDown(0))
                .bool("mouse.right_down", Mouse.isButtonDown(1))
                .build();
    }

    static double scaledMouseX(Minecraft client, int scaledWidth) {
        if (client.displayWidth <= 0) return 0.0D;
        return finite((double) Mouse.getX() * scaledWidth / client.displayWidth);
    }
    static double scaledMouseY(Minecraft client, int scaledHeight) {
        if (client.displayHeight <= 0) return 0.0D;
        return finite(scaledHeight - (double) Mouse.getY() * scaledHeight / client.displayHeight - 1.0D);
    }
    static double finite(double value) {
        return Double.isNaN(value) || Double.isInfinite(value) ? 0.0D : value;
    }
}
