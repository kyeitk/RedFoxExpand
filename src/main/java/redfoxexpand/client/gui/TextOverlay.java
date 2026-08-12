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

    public void renderAnchored(int guiLeft, int guiTop, int guiWidth, int guiHeight,
                               int screenWidth, int screenHeight,
                               boolean matrixAtGuiOrigin) {
        int originX = Math.round(resolveAnchorX(guiLeft, guiWidth, screenWidth));
        int originY = Math.round(resolveAnchorY(guiTop, guiHeight, screenHeight));
        if (matrixAtGuiOrigin) { originX -= guiLeft; originY -= guiTop; }
        render(originX, originY);
    }

    private float resolveAnchorX(int guiLeft, int guiWidth, int screenWidth) {
        if (anchor == SpriteOverlay.Anchor.GUI || anchor == SpriteOverlay.Anchor.GUI_TOP_LEFT) return guiLeft;
        if (anchor == SpriteOverlay.Anchor.SCREEN || anchor == SpriteOverlay.Anchor.SCREEN_TOP_LEFT
                || anchor == SpriteOverlay.Anchor.PARENT) return 0.0F;
        if (anchor == SpriteOverlay.Anchor.SCREEN_CENTER) return screenWidth / 2.0F;
        boolean gui = anchor.name().startsWith("GUI_");
        float left = gui ? guiLeft : 0.0F;
        float width = gui ? guiWidth : screenWidth;
        if (anchor.name().endsWith("_LEFT")) return left;
        if (anchor.name().endsWith("_RIGHT")) return left + width;
        return left + width / 2.0F;
    }

    private float resolveAnchorY(int guiTop, int guiHeight, int screenHeight) {
        if (anchor == SpriteOverlay.Anchor.GUI || anchor == SpriteOverlay.Anchor.GUI_TOP_LEFT) return guiTop;
        if (anchor == SpriteOverlay.Anchor.SCREEN || anchor == SpriteOverlay.Anchor.SCREEN_TOP_LEFT
                || anchor == SpriteOverlay.Anchor.PARENT) return 0.0F;
        if (anchor == SpriteOverlay.Anchor.SCREEN_CENTER) return screenHeight / 2.0F;
        boolean gui = anchor.name().startsWith("GUI_");
        float top = gui ? guiTop : 0.0F;
        float height = gui ? guiHeight : screenHeight;
        if (anchor.name().contains("_TOP_")) return top;
        if (anchor.name().contains("_BOTTOM_")) return top + height;
        return top + height / 2.0F;
    }
}

