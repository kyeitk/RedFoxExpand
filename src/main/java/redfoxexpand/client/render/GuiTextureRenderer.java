package redfoxexpand.client.render;

import redfoxexpand.client.gui.SpriteOverlay;
import redfoxexpand.client.gui.ReactiveScreenRuntime;
import redfoxexpand.client.gui.SceneRenderState;
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
            int guiWidth,
            int guiHeight,
            int screenWidth,
            int screenHeight,
        boolean matrixAtGuiOrigin,
        ReactiveScreenRuntime reactiveRuntime
    ) {
        long nowMillis = Forge1710Clock.INSTANCE.nowMillis();
        for (SpriteOverlay sprite : sprites) {
            if (sprite.layer != layer) continue;
            SceneRenderState scene = reactiveRuntime == null || sprite.nativeSprite == null
                    ? null : reactiveRuntime.scene(sprite.nativeSprite, nowMillis);
            if (scene != null) {
                renderScene(sprite, scene, guiLeft, guiTop, guiWidth, guiHeight,
                        screenWidth, screenHeight, matrixAtGuiOrigin, nowMillis);
                continue;
            }
            FinalRenderProperties properties = reactiveRuntime == null
                    ? FinalRenderProperties.BASE
                    : reactiveRuntime.properties(sprite.nativeSprite, nowMillis);
            render(sprite, guiLeft, guiTop, guiWidth, guiHeight, screenWidth, screenHeight,
                    matrixAtGuiOrigin, nowMillis, properties);
        }
    }

    private static void render(
            SpriteOverlay sprite,
            int guiLeft,
            int guiTop,
            int guiWidth,
            int guiHeight,
            int screenWidth,
            int screenHeight,
            boolean matrixAtGuiOrigin,
            long nowMillis,
            FinalRenderProperties properties
    ) {
        if (!properties.isVisible()) return;
        float renderX = sprite.resolveRenderX(guiLeft, guiWidth, screenWidth, matrixAtGuiOrigin)
                + (float) properties.getTranslateX();
        float renderY = sprite.resolveRenderY(guiTop, guiHeight, screenHeight, matrixAtGuiOrigin)
                + (float) properties.getTranslateY();
        AlphaBlendState state = AlphaBlendState.begin();
        boolean matrixPushed = false;
        try {
            GL11.glPushMatrix();
            matrixPushed = true;
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
            if (matrixPushed) GL11.glPopMatrix();
            state.close();
        }
    }

    private static void renderScene(SpriteOverlay sprite, SceneRenderState scene,
                                    int guiLeft, int guiTop, int guiWidth, int guiHeight,
                                    int screenWidth, int screenHeight, boolean matrixAtGuiOrigin,
                                    long nowMillis) {
        double alpha = 1.0D;
        AlphaBlendState state = AlphaBlendState.begin();
        boolean matrixPushed = false;
        try {
            GL11.glPushMatrix();
            matrixPushed = true;
            boolean root = true;
            for (SceneRenderState.Node node : scene.nodes()) {
                FinalRenderProperties properties = node.properties;
                if (!properties.isVisible()) return;
                alpha *= properties.getAlpha();
                if (root) {
                    float anchorX = resolveAnchorX(node.anchor, guiLeft, guiWidth, screenWidth);
                    float anchorY = resolveAnchorY(node.anchor, guiTop, guiHeight, screenHeight);
                    if (matrixAtGuiOrigin) { anchorX -= guiLeft; anchorY -= guiTop; }
                    GL11.glTranslatef(anchorX, anchorY, 0.0F);
                    root = false;
                }
                float pivotX = (float) node.pivot.x();
                float pivotY = (float) node.pivot.y();
                GL11.glTranslatef((float) (node.x + properties.getTranslateX()) + pivotX,
                        (float) (node.y + properties.getTranslateY()) + pivotY, 0.0F);
                GL11.glRotatef((float) properties.getRotationZ(), 0.0F, 0.0F, 1.0F);
                GL11.glScalef((float) properties.getScaleX(),
                        (float) properties.getScaleY(), 1.0F);
                GL11.glTranslatef(-pivotX, -pivotY, 0.0F);
            }
            if (alpha <= 0.0D) return;
            float colorAlpha = ((sprite.color >>> 24) & 0xFF) / 255.0F;
            GL11.glColor4f(((sprite.color >>> 16) & 0xFF) / 255.0F,
                    ((sprite.color >>> 8) & 0xFF) / 255.0F,
                    (sprite.color & 0xFF) / 255.0F, colorAlpha * (float) alpha);
            TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
            ResourceLocation texture = sprite.texture.textureAt(nowMillis);
            NativeTextureCache.bind(textureManager, texture);
            drawTexturedQuad(0.0F, 0.0F, sprite.width, sprite.height,
                    sprite.minU(), sprite.minV(), sprite.maxU(), sprite.maxV());
        } finally {
            if (matrixPushed) GL11.glPopMatrix();
            state.close();
        }
    }

    public static float resolveAnchorX(redfoxexpand.core.GuiDefinition.Anchor anchor,
                                       int guiLeft, int guiWidth, int screenWidth) {
        if (anchor == redfoxexpand.core.GuiDefinition.Anchor.GUI
                || anchor == redfoxexpand.core.GuiDefinition.Anchor.GUI_TOP_LEFT) return guiLeft;
        if (anchor == redfoxexpand.core.GuiDefinition.Anchor.SCREEN
                || anchor == redfoxexpand.core.GuiDefinition.Anchor.SCREEN_TOP_LEFT
                || anchor == redfoxexpand.core.GuiDefinition.Anchor.PARENT) return 0.0F;
        if (anchor == redfoxexpand.core.GuiDefinition.Anchor.SCREEN_CENTER) return screenWidth / 2.0F;
        boolean gui = anchor.name().startsWith("GUI_");
        float left = gui ? guiLeft : 0.0F;
        float width = gui ? guiWidth : screenWidth;
        if (anchor.name().endsWith("_LEFT")) return left;
        if (anchor.name().endsWith("_RIGHT")) return left + width;
        return left + width / 2.0F;
    }

    public static float resolveAnchorY(redfoxexpand.core.GuiDefinition.Anchor anchor,
                                       int guiTop, int guiHeight, int screenHeight) {
        if (anchor == redfoxexpand.core.GuiDefinition.Anchor.GUI
                || anchor == redfoxexpand.core.GuiDefinition.Anchor.GUI_TOP_LEFT) return guiTop;
        if (anchor == redfoxexpand.core.GuiDefinition.Anchor.SCREEN
                || anchor == redfoxexpand.core.GuiDefinition.Anchor.SCREEN_TOP_LEFT
                || anchor == redfoxexpand.core.GuiDefinition.Anchor.PARENT) return 0.0F;
        if (anchor == redfoxexpand.core.GuiDefinition.Anchor.SCREEN_CENTER) return screenHeight / 2.0F;
        boolean gui = anchor.name().startsWith("GUI_");
        float top = gui ? guiTop : 0.0F;
        float height = gui ? guiHeight : screenHeight;
        if (anchor.name().contains("_TOP_")) return top;
        if (anchor.name().contains("_BOTTOM_")) return top + height;
        return top + height / 2.0F;
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
