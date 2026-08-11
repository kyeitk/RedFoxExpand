package redfoxexpand.client.gui;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import redfoxexpand.core.GuiDefinition;

public final class TextOverlay {

    public final String text;
    public final int x;
    public final int y;
    public final int color;
    public final boolean shadow;
    public final boolean translate;
    public final SpriteOverlay.Layer layer;
    public final SpriteOverlay.Anchor anchor;

    private TextOverlay(String text, int x, int y, int color, boolean shadow, boolean translate,
                        SpriteOverlay.Layer layer, SpriteOverlay.Anchor anchor) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.color = color;
        this.shadow = shadow;
        this.translate = translate;
        this.layer = layer;
        this.anchor = anchor;
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
                JsonSupport.bool(json, "translate", false),
                SpriteOverlay.Layer.parse(JsonSupport.string(json, "layer", "foreground")),
                SpriteOverlay.Anchor.parse(JsonSupport.string(json, "anchor", "gui"))
        );
    }

    public static TextOverlay fromNative(GuiDefinition.TextOverlay text) {
        return new TextOverlay(text.text(), text.x(), text.y(), text.color(),
                text.shadow(), text.translate(),
                SpriteOverlay.Layer.valueOf(text.layer().name()),
                SpriteOverlay.Anchor.valueOf(text.anchor().name()));
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

    public void renderAnchored(int guiLeft, int guiTop, int screenWidth, int screenHeight,
                               boolean matrixAtGuiOrigin) {
        int originX = anchor == SpriteOverlay.Anchor.SCREEN_CENTER ? screenWidth / 2
                : anchor == SpriteOverlay.Anchor.SCREEN ? 0 : guiLeft;
        int originY = anchor == SpriteOverlay.Anchor.SCREEN_CENTER ? screenHeight / 2
                : anchor == SpriteOverlay.Anchor.SCREEN ? 0 : guiTop;
        if (matrixAtGuiOrigin) { originX -= guiLeft; originY -= guiTop; }
        render(originX, originY);
    }
}
