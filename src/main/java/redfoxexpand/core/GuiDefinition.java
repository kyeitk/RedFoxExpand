package redfoxexpand.core;

import redfoxexpand.reactive.ReactiveDefinition;

import java.util.List;
import java.util.Set;

/** Immutable strict Definition body shared by Schema v2 and additive Schema v3. */
public record GuiDefinition(
        Geometry geometry,
        List<SlotRule> slotRules,
        List<Sprite> sprites,
        List<TextOverlay> texts,
        List<TextRule> textRules,
        ReactiveDefinition reactive
) {
    public GuiDefinition {
        slotRules = List.copyOf(slotRules);
        sprites = List.copyOf(sprites);
        texts = List.copyOf(texts);
        textRules = List.copyOf(textRules);
        if (reactive == null) reactive = ReactiveDefinition.EMPTY;
    }

    /** Schema v2 compatibility constructor: v2 definitions have no reactive extension. */
    public GuiDefinition(Geometry geometry, List<SlotRule> slotRules, List<Sprite> sprites,
                         List<TextOverlay> texts, List<TextRule> textRules) {
        this(geometry, slotRules, sprites, texts, textRules, ReactiveDefinition.EMPTY);
    }

    public record Geometry(int xOffset, int yOffset, int widthOffset, int heightOffset) {
        public static final Geometry ZERO = new Geometry(0, 0, 0, 0);
    }

    public enum ClassMatchMode { EXACT, ASSIGNABLE }
    public enum ClassNameType { FULL, SIMPLE }
    public enum Layer { UNDERLAY, BACKGROUND, FOREGROUND }
    public enum Anchor { GUI, SCREEN_CENTER, SCREEN }
    public enum ResourceType { RESOURCE_LOCATION, GUI_SPRITE, PACK_RESOURCE }
    public enum MissingFrameBehavior { USE_DEFAULT, SKIP, DISABLE }
    public enum AnimationCondition { ALWAYS, NEVER }
    public enum TextSelector { TITLE, PLAYER_INVENTORY }

    public record SlotRule(
            Set<Integer> slots,
            Integer targetX,
            Integer targetY,
            String targetClass,
            ClassMatchMode classMatchMode,
            ClassNameType classNameType,
            int xOffset,
            int yOffset,
            Integer highlightColor,
            Integer highlightColor2
    ) {
        public SlotRule { slots = Set.copyOf(slots); }

        public boolean matches(GuiContext.SlotContext slot) {
            if (!slots.isEmpty() && !slots.contains(slot.index())) return false;
            if (targetX != null && targetX != slot.x()) return false;
            if (targetY != null && targetY != slot.y()) return false;
            if (targetClass == null) return true;
            List<String> hierarchy = classNameType == ClassNameType.FULL
                    ? slot.hierarchy() : slot.simpleHierarchy();
            String exact = classNameType == ClassNameType.FULL
                    ? slot.className() : slot.simpleClassName();
            return classMatchMode == ClassMatchMode.EXACT
                    ? targetClass.equals(exact) : hierarchy.contains(targetClass);
        }
    }

    public record TextureSpec(ResourceType type, String location) { }

    public record AnimationFrame(TextureSpec texture, int durationMillis) { }

    public record Animation(
            List<AnimationFrame> frames,
            boolean loop,
            AnimationCondition condition,
            TextureSpec defaultTexture,
            MissingFrameBehavior missingFrameBehavior
    ) {
        public Animation { frames = List.copyOf(frames); }
    }

    public record Sprite(
            TextureSpec texture,
            Animation animation,
            double x,
            double y,
            double z,
            double u,
            double v,
            double width,
            double height,
            double sourceWidth,
            double sourceHeight,
            double textureWidth,
            double textureHeight,
            boolean fullTexture,
            int color,
            Layer layer,
            Anchor anchor,
            String id
    ) {
        /** Schema v2 compatibility constructor: v2 sprites intentionally have no stable element ID. */
        public Sprite(TextureSpec texture, Animation animation, double x, double y, double z,
                      double u, double v, double width, double height, double sourceWidth,
                      double sourceHeight, double textureWidth, double textureHeight,
                      boolean fullTexture, int color, Layer layer, Anchor anchor) {
            this(texture, animation, x, y, z, u, v, width, height, sourceWidth, sourceHeight,
                    textureWidth, textureHeight, fullTexture, color, layer, anchor, null);
        }
    }

    public record TextOverlay(
            String text,
            boolean translate,
            int x,
            int y,
            int color,
            boolean shadow,
            Layer layer,
            Anchor anchor
    ) { }

    public record TextRule(
            TextSelector selector,
            int xOffset,
            int yOffset,
            Integer color,
            boolean hidden
    ) { }
}
