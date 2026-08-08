package redfoxexpand.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import redfoxexpand.core.GuiDefinition;

import java.util.List;

/** Backend-neutral GUI extraction through Blaze3D RenderPipeline APIs only. */
final class GuiTextureRenderer {
    private GuiTextureRenderer() {
    }

    static void renderLayer(GuiRuntimeState state, GuiGraphicsExtractor graphics,
                            GuiDefinition.Layer layer, int mouseX, int mouseY) {
        if (state.modifier == null || state.snapshot == null) return;
        long now = System.currentTimeMillis();
        for (GuiDefinition.Sprite sprite : state.modifier.sprites()) {
            if (sprite.layer() == layer) renderSprite(state, graphics, sprite, now);
        }
        for (GuiDefinition.TextOverlay text : state.modifier.texts()) {
            if (text.layer() == layer) renderText(state, graphics, text);
        }
        if (layer == GuiDefinition.Layer.FOREGROUND) renderSlotHighlight(state, graphics, mouseX, mouseY);
    }

    private static void renderSprite(GuiRuntimeState state, GuiGraphicsExtractor graphics,
                                     GuiDefinition.Sprite sprite, long now) {
        Identifier texture = textureAt(state, sprite, now);
        if (texture == null) return;
        int x = Math.round((float) (anchorX(state, sprite.anchor()) + sprite.x()));
        int y = Math.round((float) (anchorY(state, sprite.anchor()) + sprite.y()));
        int width = Math.max(1, Math.round((float) sprite.width()));
        int height = Math.max(1, Math.round((float) sprite.height()));
        if (sprite.texture().type() == GuiDefinition.ResourceType.GUI_SPRITE) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, texture, x, y, width, height, sprite.color());
        } else if (sprite.fullTexture()) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F,
                    width, height, width, height, width, height, sprite.color());
        } else {
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y,
                    (float) sprite.u(), (float) sprite.v(), width, height,
                    Math.max(1, Math.round((float) sprite.sourceWidth())),
                    Math.max(1, Math.round((float) sprite.sourceHeight())),
                    Math.max(1, Math.round((float) sprite.textureWidth())),
                    Math.max(1, Math.round((float) sprite.textureHeight())), sprite.color());
        }
    }

    private static Identifier textureAt(GuiRuntimeState state, GuiDefinition.Sprite sprite, long now) {
        GuiDefinition.Animation animation = sprite.animation();
        if (animation == null) return state.snapshot.texture(sprite.texture());
        Identifier fallback = state.snapshot.texture(animation.defaultTexture());
        if (animation.condition() == GuiDefinition.AnimationCondition.NEVER) return fallback;
        if (animation.missingFrameBehavior() == GuiDefinition.MissingFrameBehavior.DISABLE) {
            for (GuiDefinition.AnimationFrame frame : animation.frames()) {
                if (state.snapshot.texture(frame.texture()) == null) return fallback;
            }
        }
        long total = 0L;
        for (GuiDefinition.AnimationFrame frame : animation.frames()) {
            if (state.snapshot.texture(frame.texture()) != null
                    || animation.missingFrameBehavior() == GuiDefinition.MissingFrameBehavior.USE_DEFAULT) {
                total += frame.durationMillis();
            }
        }
        if (total <= 0) return fallback;
        long elapsed = Math.max(0L, now - state.snapshot.startedAtMillis());
        if (!animation.loop() && elapsed >= total) return fallback;
        long position = animation.loop() ? elapsed % total : elapsed;
        for (GuiDefinition.AnimationFrame frame : animation.frames()) {
            Identifier texture = state.snapshot.texture(frame.texture());
            if (texture == null && animation.missingFrameBehavior() == GuiDefinition.MissingFrameBehavior.SKIP) continue;
            if (position < frame.durationMillis()) return texture == null ? fallback : texture;
            position -= frame.durationMillis();
        }
        return fallback;
    }

    private static void renderText(GuiRuntimeState state, GuiGraphicsExtractor graphics,
                                   GuiDefinition.TextOverlay text) {
        int x = anchorX(state, text.anchor()) + text.x();
        int y = anchorY(state, text.anchor()) + text.y();
        Component component = text.translate() ? Component.translatable(text.text()) : Component.literal(text.text());
        graphics.text(Minecraft.getInstance().font, component, x, y, text.color(), text.shadow());
    }

    private static void renderSlotHighlight(GuiRuntimeState state, GuiGraphicsExtractor graphics,
                                            int mouseX, int mouseY) {
        List<Slot> slots = state.screen.getMenu().slots;
        List<redfoxexpand.core.GuiContext.SlotContext> bases = state.baseContext.slots();
        for (int index = 0; index < Math.min(slots.size(), bases.size()); index++) {
            Slot slot = slots.get(index);
            int x = state.baseContext.leftPos() + state.leftDelta + slot.x;
            int y = state.baseContext.topPos() + state.topDelta + slot.y;
            if (mouseX < x || mouseX >= x + 16 || mouseY < y || mouseY >= y + 16) continue;
            for (GuiDefinition.SlotRule rule : state.modifier.slotRules()) {
                if (rule.matches(bases.get(index)) && (rule.highlightColor() != null || rule.highlightColor2() != null)) {
                    int first = rule.highlightColor() == null ? 0x80FFFFFF : rule.highlightColor();
                    int second = rule.highlightColor2() == null ? first : rule.highlightColor2();
                    graphics.fillGradient(x, y, x + 16, y + 16, first, second);
                    return;
                }
            }
        }
    }

    private static int anchorX(GuiRuntimeState state, GuiDefinition.Anchor anchor) {
        return switch (anchor) {
            case GUI -> state.baseContext.leftPos() + state.leftDelta;
            case SCREEN_CENTER -> state.baseContext.screenWidth() / 2;
            case SCREEN -> 0;
        };
    }

    private static int anchorY(GuiRuntimeState state, GuiDefinition.Anchor anchor) {
        return switch (anchor) {
            case GUI -> state.baseContext.topPos() + state.topDelta;
            case SCREEN_CENTER -> state.baseContext.screenHeight() / 2;
            case SCREEN -> 0;
        };
    }
}
