package redfoxexpand.core;

import redfoxexpand.reactive.ReactiveDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Immutable strict Definition body shared by Schema v2 and additive Schema v3. */
public final class GuiDefinition {
    private final Geometry geometry;
    private final List<SlotRule> slotRules;
    private final List<Sprite> sprites;
    private final List<TextOverlay> texts;
    private final List<TextRule> textRules;
    private final ReactiveDefinition reactive;

    public GuiDefinition(Geometry geometry, List<SlotRule> slotRules, List<Sprite> sprites,
                         List<TextOverlay> texts, List<TextRule> textRules) {
        this(geometry, slotRules, sprites, texts, textRules, ReactiveDefinition.EMPTY);
    }
    public GuiDefinition(Geometry geometry, List<SlotRule> slotRules, List<Sprite> sprites,
                         List<TextOverlay> texts, List<TextRule> textRules,
                         ReactiveDefinition reactive) {
        this.geometry = geometry;
        this.slotRules = immutable(slotRules); this.sprites = immutable(sprites);
        this.texts = immutable(texts); this.textRules = immutable(textRules);
        this.reactive = reactive == null ? ReactiveDefinition.EMPTY : reactive;
    }
    private static <T> List<T> immutable(List<T> values) {
        return Collections.unmodifiableList(new ArrayList<T>(values));
    }
    public Geometry geometry() { return geometry; }
    public List<SlotRule> slotRules() { return slotRules; }
    public List<Sprite> sprites() { return sprites; }
    public List<TextOverlay> texts() { return texts; }
    public List<TextRule> textRules() { return textRules; }
    public ReactiveDefinition reactive() { return reactive; }

    public static final class Geometry {
        public static final Geometry ZERO = new Geometry(0, 0, 0, 0);
        private final int xOffset, yOffset, widthOffset, heightOffset;
        public Geometry(int xOffset, int yOffset, int widthOffset, int heightOffset) {
            this.xOffset = xOffset; this.yOffset = yOffset;
            this.widthOffset = widthOffset; this.heightOffset = heightOffset;
        }
        public int xOffset() { return xOffset; }
        public int yOffset() { return yOffset; }
        public int widthOffset() { return widthOffset; }
        public int heightOffset() { return heightOffset; }
        @Override public boolean equals(Object other) {
            if (!(other instanceof Geometry)) return false;
            Geometry that = (Geometry) other;
            return xOffset == that.xOffset && yOffset == that.yOffset
                    && widthOffset == that.widthOffset && heightOffset == that.heightOffset;
        }
        @Override public int hashCode() {
            int value = xOffset; value = 31 * value + yOffset;
            value = 31 * value + widthOffset; return 31 * value + heightOffset;
        }
    }

    public enum ClassMatchMode { EXACT, ASSIGNABLE }
    public enum ClassNameType { FULL, SIMPLE }
    public enum Layer { UNDERLAY, BACKGROUND, FOREGROUND }
    public enum Anchor { GUI, SCREEN_CENTER, SCREEN }
    public enum ResourceType { RESOURCE_LOCATION, GUI_SPRITE, PACK_RESOURCE }
    public enum MissingFrameBehavior { USE_DEFAULT, SKIP, DISABLE }
    public enum AnimationCondition { ALWAYS, NEVER }
    public enum TextSelector { TITLE, PLAYER_INVENTORY }

    public static final class SlotRule {
        private final Set<Integer> slots;
        private final Integer targetX, targetY;
        private final String targetClass;
        private final ClassMatchMode classMatchMode;
        private final ClassNameType classNameType;
        private final int xOffset, yOffset;
        private final Integer highlightColor, highlightColor2;
        public SlotRule(Set<Integer> slots, Integer targetX, Integer targetY,
                        String targetClass, ClassMatchMode classMatchMode,
                        ClassNameType classNameType, int xOffset, int yOffset,
                        Integer highlightColor, Integer highlightColor2) {
            this.slots = Collections.unmodifiableSet(new HashSet<Integer>(slots));
            this.targetX = targetX; this.targetY = targetY; this.targetClass = targetClass;
            this.classMatchMode = classMatchMode; this.classNameType = classNameType;
            this.xOffset = xOffset; this.yOffset = yOffset;
            this.highlightColor = highlightColor; this.highlightColor2 = highlightColor2;
        }
        public Set<Integer> slots() { return slots; }
        public Integer targetX() { return targetX; }
        public Integer targetY() { return targetY; }
        public String targetClass() { return targetClass; }
        public ClassMatchMode classMatchMode() { return classMatchMode; }
        public ClassNameType classNameType() { return classNameType; }
        public int xOffset() { return xOffset; }
        public int yOffset() { return yOffset; }
        public Integer highlightColor() { return highlightColor; }
        public Integer highlightColor2() { return highlightColor2; }
        public boolean matches(GuiContext.SlotContext slot) {
            if (!slots.isEmpty() && !slots.contains(slot.index())) return false;
            if (targetX != null && targetX.intValue() != slot.x()) return false;
            if (targetY != null && targetY.intValue() != slot.y()) return false;
            if (targetClass == null) return true;
            List<String> hierarchy = classNameType == ClassNameType.FULL
                    ? slot.hierarchy() : slot.simpleHierarchy();
            String exact = classNameType == ClassNameType.FULL
                    ? slot.className() : slot.simpleClassName();
            return classMatchMode == ClassMatchMode.EXACT
                    ? targetClass.equals(exact) : hierarchy.contains(targetClass);
        }
    }

    public static final class TextureSpec {
        private final ResourceType type; private final String location;
        public TextureSpec(ResourceType type, String location) { this.type = type; this.location = location; }
        public ResourceType type() { return type; }
        public String location() { return location; }
        @Override public boolean equals(Object o) {
            if (!(o instanceof TextureSpec)) return false;
            TextureSpec that = (TextureSpec) o;
            return type == that.type && location.equals(that.location);
        }
        @Override public int hashCode() { return 31 * type.hashCode() + location.hashCode(); }
    }
    public static final class AnimationFrame {
        private final TextureSpec texture; private final int durationMillis;
        public AnimationFrame(TextureSpec texture, int durationMillis) {
            this.texture = texture; this.durationMillis = durationMillis;
        }
        public TextureSpec texture() { return texture; }
        public int durationMillis() { return durationMillis; }
    }
    public static final class Animation {
        private final List<AnimationFrame> frames;
        private final boolean loop;
        private final AnimationCondition condition;
        private final TextureSpec defaultTexture;
        private final MissingFrameBehavior missingFrameBehavior;
        public Animation(List<AnimationFrame> frames, boolean loop, AnimationCondition condition,
                         TextureSpec defaultTexture, MissingFrameBehavior missingFrameBehavior) {
            this.frames = immutable(frames); this.loop = loop; this.condition = condition;
            this.defaultTexture = defaultTexture; this.missingFrameBehavior = missingFrameBehavior;
        }
        public List<AnimationFrame> frames() { return frames; }
        public boolean loop() { return loop; }
        public AnimationCondition condition() { return condition; }
        public TextureSpec defaultTexture() { return defaultTexture; }
        public MissingFrameBehavior missingFrameBehavior() { return missingFrameBehavior; }
    }
    public static final class Sprite {
        private final TextureSpec texture; private final Animation animation;
        private final double x, y, z, u, v, width, height, sourceWidth, sourceHeight;
        private final double textureWidth, textureHeight;
        private final boolean fullTexture; private final int color;
        private final Layer layer; private final Anchor anchor; private final String id;
        public Sprite(TextureSpec texture, Animation animation, double x, double y, double z,
                      double u, double v, double width, double height, double sourceWidth,
                      double sourceHeight, double textureWidth, double textureHeight,
                      boolean fullTexture, int color, Layer layer, Anchor anchor) {
            this(texture, animation, x, y, z, u, v, width, height, sourceWidth, sourceHeight,
                    textureWidth, textureHeight, fullTexture, color, layer, anchor, null);
        }
        public Sprite(TextureSpec texture, Animation animation, double x, double y, double z,
                      double u, double v, double width, double height, double sourceWidth,
                      double sourceHeight, double textureWidth, double textureHeight,
                      boolean fullTexture, int color, Layer layer, Anchor anchor, String id) {
            this.texture = texture; this.animation = animation; this.x = x; this.y = y; this.z = z;
            this.u = u; this.v = v; this.width = width; this.height = height;
            this.sourceWidth = sourceWidth; this.sourceHeight = sourceHeight;
            this.textureWidth = textureWidth; this.textureHeight = textureHeight;
            this.fullTexture = fullTexture; this.color = color; this.layer = layer;
            this.anchor = anchor; this.id = id;
        }
        public TextureSpec texture() { return texture; }
        public Animation animation() { return animation; }
        public double x() { return x; } public double y() { return y; } public double z() { return z; }
        public double u() { return u; } public double v() { return v; }
        public double width() { return width; } public double height() { return height; }
        public double sourceWidth() { return sourceWidth; } public double sourceHeight() { return sourceHeight; }
        public double textureWidth() { return textureWidth; } public double textureHeight() { return textureHeight; }
        public boolean fullTexture() { return fullTexture; }
        public int color() { return color; }
        public Layer layer() { return layer; }
        public Anchor anchor() { return anchor; }
        public String id() { return id; }
    }
    public static final class TextOverlay {
        private final String text; private final boolean translate;
        private final int x, y, color; private final boolean shadow;
        private final Layer layer; private final Anchor anchor;
        public TextOverlay(String text, boolean translate, int x, int y, int color,
                           boolean shadow, Layer layer, Anchor anchor) {
            this.text = text; this.translate = translate; this.x = x; this.y = y;
            this.color = color; this.shadow = shadow; this.layer = layer; this.anchor = anchor;
        }
        public String text() { return text; } public boolean translate() { return translate; }
        public int x() { return x; } public int y() { return y; } public int color() { return color; }
        public boolean shadow() { return shadow; } public Layer layer() { return layer; }
        public Anchor anchor() { return anchor; }
    }
    public static final class TextRule {
        private final TextSelector selector; private final int xOffset, yOffset;
        private final Integer color; private final boolean hidden;
        public TextRule(TextSelector selector, int xOffset, int yOffset, Integer color, boolean hidden) {
            this.selector = selector; this.xOffset = xOffset; this.yOffset = yOffset;
            this.color = color; this.hidden = hidden;
        }
        public TextSelector selector() { return selector; }
        public int xOffset() { return xOffset; } public int yOffset() { return yOffset; }
        public Integer color() { return color; } public boolean hidden() { return hidden; }
    }
}

