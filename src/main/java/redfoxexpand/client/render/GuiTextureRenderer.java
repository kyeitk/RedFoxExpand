package redfoxexpand.client.render;

import redfoxexpand.client.gui.SpriteOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.renderer.GlStateManager;
import redfoxexpand.client.gui.ReactiveScreenRuntime;
import redfoxexpand.client.resource.NativeTextureCache;
import redfoxexpand.platform.forge189.Forge189Clock;
import redfoxexpand.reactive.property.FinalRenderProperties;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

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
            boolean matrixAtGuiOrigin,
            ReactiveScreenRuntime reactiveRuntime
    ) {
        long nowMillis = Forge189Clock.INSTANCE.nowMillis();
        for (SpriteOverlay sprite : sprites) {
            if (sprite.layer == layer) {
                FinalRenderProperties properties = reactiveRuntime == null
                        ? FinalRenderProperties.BASE : reactiveRuntime.properties(sprite, nowMillis);
                render(sprite, guiLeft, guiTop, screenWidth, screenHeight,
                        matrixAtGuiOrigin, nowMillis, properties);
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
            long nowMillis,
            FinalRenderProperties properties
    ) {
        if (!properties.isVisible()) return;
        float renderX = sprite.resolveRenderX(guiLeft, screenWidth, matrixAtGuiOrigin)
                + (float) properties.getTranslateX();
        float renderY = sprite.resolveRenderY(guiTop, screenHeight, matrixAtGuiOrigin)
                + (float) properties.getTranslateY();

        AlphaBlendState state = AlphaBlendState.begin();
        boolean matrixPushed = false;
        try {
            GlStateManager.pushMatrix();
            matrixPushed = true;
            float centerX = renderX + sprite.width / 2.0F;
            float centerY = renderY + sprite.height / 2.0F;
            GlStateManager.translate(centerX, centerY, 0.0F);
            GlStateManager.rotate((float) properties.getRotationZ(), 0.0F, 0.0F, 1.0F);
            GlStateManager.scale((float) properties.getScaleX(),
                    (float) properties.getScaleY(), 1.0F);
            GlStateManager.translate(-centerX, -centerY, 0.0F);
            float colorAlpha = ((sprite.color >>> 24) & 0xFF) / 255.0F;
            GlStateManager.color(((sprite.color >>> 16) & 0xFF) / 255.0F,
                    ((sprite.color >>> 8) & 0xFF) / 255.0F,
                    (sprite.color & 0xFF) / 255.0F,
                    colorAlpha * (float) properties.getAlpha());
            TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
            ResourceLocation texture = sprite.texture.textureAt(nowMillis);
            if (sprite.isNative()) NativeTextureCache.bind(textureManager, texture);
            else textureManager.bindTexture(texture);
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
            if (matrixPushed) GlStateManager.popMatrix();
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
