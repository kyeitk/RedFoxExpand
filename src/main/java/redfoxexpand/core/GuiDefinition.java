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
        ReactiveDefinition reactive,
        List<Group> groups
) {
    public GuiDefinition {
        slotRules = List.copyOf(slotRules);
        sprites = List.copyOf(sprites);
        texts = List.copyOf(texts);
        textRules = List.copyOf(textRules);
        if (reactive == null) reactive = ReactiveDefinition.EMPTY;
        groups = List.copyOf(groups);
    }

    /** Schema v2 compatibility constructor: v2 definitions have no reactive extension. */
    public GuiDefinition(Geometry geometry, List<SlotRule> slotRules, List<Sprite> sprites,
                         List<TextOverlay> texts, List<TextRule> textRules) {
        this(geometry, slotRules, sprites, texts, textRules, ReactiveDefinition.EMPTY, List.of());
    }

    /** Schema v3 compatibility constructor: v3 has reactive sprites but no scene groups. */
    public GuiDefinition(Geometry geometry, List<SlotRule> slotRules, List<Sprite> sprites,
                         List<TextOverlay> texts, List<TextRule> textRules,
                         ReactiveDefinition reactive) {
        this(geometry, slotRules, sprites, texts, textRules, reactive, List.of());
    }

    public record Geometry(int xOffset, int yOffset, int widthOffset, int heightOffset) {
        public static final Geometry ZERO = new Geometry(0, 0, 0, 0);
    }

    public enum ClassMatchMode { EXACT, ASSIGNABLE }
    public enum ClassNameType { FULL, SIMPLE }
    public enum Layer { UNDERLAY, BACKGROUND, FOREGROUND }
    public enum Anchor {
        GUI,
        SCREEN_CENTER,
        SCREEN,
        GUI_TOP_LEFT,
        GUI_TOP_CENTER,
        GUI_TOP_RIGHT,
        GUI_CENTER_LEFT,
        GUI_CENTER,
        GUI_CENTER_RIGHT,
        GUI_BOTTOM_LEFT,
        GUI_BOTTOM_CENTER,
        GUI_BOTTOM_RIGHT,
        SCREEN_TOP_LEFT,
        SCREEN_TOP_CENTER,
        SCREEN_TOP_RIGHT,
        SCREEN_CENTER_LEFT,
        SCREEN_CENTER_RIGHT,
        SCREEN_BOTTOM_LEFT,
        SCREEN_BOTTOM_CENTER,
        SCREEN_BOTTOM_RIGHT,
        PARENT
    }
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
            String id,
            String parentId,
            Pivot pivot,
            int sceneOrder,
            boolean sceneManaged
    ) {
        public Sprite {
            if (pivot == null) pivot = new Pivot(width * 0.5D, height * 0.5D);
        }

        /** Schema v3 compatibility constructor: stable ID, no Scene Graph. */
        public Sprite(TextureSpec texture, Animation animation, double x, double y, double z,
                      double u, double v, double width, double height, double sourceWidth,
                      double sourceHeight, double textureWidth, double textureHeight,
                      boolean fullTexture, int color, Layer layer, Anchor anchor, String id) {
            this(texture, animation, x, y, z, u, v, width, height, sourceWidth, sourceHeight,
                    textureWidth, textureHeight, fullTexture, color, layer, anchor, id,
                    null, new Pivot(width * 0.5D, height * 0.5D), 0, false);
        }

        /** Schema v2 compatibility constructor: v2 sprites intentionally have no stable element ID. */
        public Sprite(TextureSpec texture, Animation animation, double x, double y, double z,
                      double u, double v, double width, double height, double sourceWidth,
                      double sourceHeight, double textureWidth, double textureHeight,
                      boolean fullTexture, int color, Layer layer, Anchor anchor) {
            this(texture, animation, x, y, z, u, v, width, height, sourceWidth, sourceHeight,
                    textureWidth, textureHeight, fullTexture, color, layer, anchor, null,
                    null, new Pivot(width * 0.5D, height * 0.5D), 0, false);
        }
    }

    public record Pivot(double x, double y) { }

    /** Non-rendering Schema v3.1 scene node. Children inherit its local transform. */
    public record Group(
            String id,
            String parentId,
            List<String> children,
            double x,
            double y,
            double width,
            double height,
            Anchor anchor,
            Pivot pivot,
            int sceneOrder
    ) {
        public Group {
            children = List.copyOf(children);
            if (pivot == null) pivot = new Pivot(width * 0.5D, height * 0.5D);
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
