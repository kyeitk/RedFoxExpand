package redfoxexpand.client.gui;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

public final class TextOverlay {

    public final String text;
    public final int x;
    public final int y;
    public final int color;
    public final boolean shadow;
    public final boolean translate;

    private TextOverlay(String text, int x, int y, int color, boolean shadow, boolean translate) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.color = color;
        this.shadow = shadow;
        this.translate = translate;
    }

    public static TextOverlay parse(JsonObject json) {
        String text = JsonSupport.string(json, "text", "");
        Integer color = JsonSupport.color(json, "color");
        return new TextOverlay(
                text,
                JsonSupport.integerAlias(json, "screen_x", "x", 0),
                JsonSupport.integerAlias(json, "screen_y", "y", 0),
                color == null ? 0xFFFFFF : color.intValue(),
                JsonSupport.bool(json, "shadow", false),
                JsonSupport.bool(json, "translate", false)
        );
    }

    public void render(int originX, int originY) {
        String rendered = translate ? I18n.format(text) : text;
        if (shadow) {
            Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(
                    rendered,
                    originX + x,
                    originY + y,
                    color
            );
        } else {
            Minecraft.getMinecraft().fontRendererObj.drawString(
                    rendered,
                    originX + x,
                    originY + y,
                    color
            );
        }
    }
}
