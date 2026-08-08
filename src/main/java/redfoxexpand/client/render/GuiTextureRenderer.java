package redfoxexpand.client.render;

import redfoxexpand.client.gui.SpriteOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import org.lwjgl.opengl.GL11;

import java.util.List;

/** Forge/Minecraft rendering code for already-parsed GUI texture definitions. */
public final class GuiTextureRenderer {

    public void renderLayer(
            List<SpriteOverlay> sprites,
            SpriteOverlay.Layer layer,
            int guiLeft,
            int guiTop,
            int screenWidth,
            int screenHeight,
        boolean matrixAtGuiOrigin
    ) {
        long nowMillis = System.currentTimeMillis();
        AlphaBlendState state = null;
        try {
            for (SpriteOverlay sprite : sprites) {
                if (sprite.layer == layer) {
                    if (state == null) {
                        state = AlphaBlendState.begin();
                    }
                    render(sprite, guiLeft, guiTop, screenWidth, screenHeight, matrixAtGuiOrigin, nowMillis);
                }
            }
        } finally {
            if (state != null) {
                state.close();
            }
        }
    }

    private static void render(
            SpriteOverlay sprite,
            int guiLeft,
            int guiTop,
            int screenWidth,
            int screenHeight,
            boolean matrixAtGuiOrigin,
            long nowMillis
    ) {
        float renderX = sprite.resolveRenderX(guiLeft, screenWidth, matrixAtGuiOrigin);
        float renderY = sprite.resolveRenderY(guiTop, screenHeight, matrixAtGuiOrigin);

        Minecraft.getMinecraft().getTextureManager().bindTexture(
                sprite.texture.textureAt(nowMillis)
        );
        drawTexturedQuad(
                renderX,
                renderY,
                sprite.width,
                sprite.height,
                sprite.minU(),
                sprite.minV(),
                sprite.maxU(),
                sprite.maxV()
        );
    }

    private static void drawTexturedQuad(
            float x,
            float y,
            float width,
            float height,
            float minU,
            float minV,
            float maxU,
            float maxV
    ) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x, y + height, 0.0D, minU, maxV);
        tessellator.addVertexWithUV(x + width, y + height, 0.0D, maxU, maxV);
        tessellator.addVertexWithUV(x + width, y, 0.0D, maxU, minV);
        tessellator.addVertexWithUV(x, y, 0.0D, minU, minV);
        tessellator.draw();
    }
}
