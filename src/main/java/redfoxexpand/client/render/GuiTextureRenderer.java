package redfoxexpand.client.render;

import redfoxexpand.client.gui.SpriteOverlay;
import redfoxexpand.client.gui.ReactiveScreenRuntime;
import redfoxexpand.client.resource.NativeTextureCache;
import redfoxexpand.platform.forge1710.Forge1710Clock;
import redfoxexpand.reactive.property.FinalRenderProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
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
        boolean matrixAtGuiOrigin,
        ReactiveScreenRuntime reactiveRuntime
    ) {
        long nowMillis = Forge1710Clock.INSTANCE.nowMillis();
        AlphaBlendState state = null;
        try {
            for (SpriteOverlay sprite : sprites) {
                if (sprite.layer == layer) {
                    FinalRenderProperties properties = reactiveRuntime == null
                            ? FinalRenderProperties.BASE
                            : reactiveRuntime.properties(sprite, nowMillis);
                    if (!properties.isVisible()) continue;
                    if (state == null) {
                        state = AlphaBlendState.begin();
                    }
                    render(sprite, guiLeft, guiTop, screenWidth, screenHeight,
                            matrixAtGuiOrigin, nowMillis, properties);
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
            long nowMillis,
            FinalRenderProperties properties
    ) {
        float renderX = sprite.resolveRenderX(guiLeft, screenWidth, matrixAtGuiOrigin)
                + (float) properties.getTranslateX();
        float renderY = sprite.resolveRenderY(guiTop, screenHeight, matrixAtGuiOrigin)
                + (float) properties.getTranslateY();
        GL11.glPushMatrix();
        try {
            float centerX = renderX + sprite.width / 2.0F;
            float centerY = renderY + sprite.height / 2.0F;
            GL11.glTranslatef(centerX, centerY, 0.0F);
            GL11.glRotatef((float) properties.getRotationZ(), 0.0F, 0.0F, 1.0F);
            GL11.glScalef((float) properties.getScaleX(),
                    (float) properties.getScaleY(), 1.0F);
            GL11.glTranslatef(-centerX, -centerY, 0.0F);
            float baseAlpha = ((sprite.color >>> 24) & 0xFF) / 255.0F;
            GL11.glColor4f(
                    ((sprite.color >>> 16) & 0xFF) / 255.0F,
                    ((sprite.color >>> 8) & 0xFF) / 255.0F,
                    (sprite.color & 0xFF) / 255.0F,
                    baseAlpha * (float) properties.getAlpha()
            );
            TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
            ResourceLocation texture = sprite.texture.textureAt(nowMillis);
            if (sprite.isNative()) {
                NativeTextureCache.bind(textureManager, texture);
            } else {
                textureManager.bindTexture(texture);
            }
            drawTexturedQuad(
                    renderX, renderY, sprite.width, sprite.height,
                    sprite.minU(), sprite.minV(), sprite.maxU(), sprite.maxV()
            );
        } finally {
            GL11.glPopMatrix();
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
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x, y + height, 0.0D, minU, maxV);
        tessellator.addVertexWithUV(x + width, y + height, 0.0D, maxU, maxV);
        tessellator.addVertexWithUV(x + width, y, 0.0D, maxU, minV);
        tessellator.addVertexWithUV(x, y, 0.0D, minU, minV);
        tessellator.draw();
    }
}
