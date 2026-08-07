package redfoxexpand.client.render;

import redfoxexpand.client.gui.SpriteOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
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
        for (SpriteOverlay sprite : sprites) {
            if (sprite.layer == layer) {
                render(sprite, guiLeft, guiTop, screenWidth, screenHeight, matrixAtGuiOrigin, nowMillis);
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

        AlphaBlendState state = AlphaBlendState.begin();
        try {
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
        } finally {
            state.close();
        }
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
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();
        renderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        renderer.pos(x, y + height, 0.0D).tex(minU, maxV).endVertex();
        renderer.pos(x + width, y + height, 0.0D).tex(maxU, maxV).endVertex();
        renderer.pos(x + width, y, 0.0D).tex(maxU, minV).endVertex();
        renderer.pos(x, y, 0.0D).tex(minU, minV).endVertex();
        tessellator.draw();
    }
}
