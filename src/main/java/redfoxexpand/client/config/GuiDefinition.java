package redfoxexpand.client.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import redfoxexpand.client.gui.ClassMatchMode;
import redfoxexpand.client.gui.JsonSupport;
import redfoxexpand.client.gui.SlotModifier;
import redfoxexpand.client.gui.SpriteOverlay;
import redfoxexpand.client.gui.TextOverlay;
import net.minecraft.util.ResourceLocation;
import redfoxexpand.core.DefinitionCandidate;
import redfoxexpand.reactive.ReactiveDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Immutable GUI configuration model, independent from Forge rendering hooks. */
public final class GuiDefinition {

    public enum TargetType {
        SCREEN_CLASS,
        CONTAINER_CLASS,
        SCREEN_TITLE;

        private static TargetType parse(String value) {
            String normalized = value.toLowerCase(Locale.ROOT);
            if ("screen_class".equals(normalized)) {
                return SCREEN_CLASS;
            }
            if ("container_class".equals(normalized) || "menu_class".equals(normalized)) {
                return CONTAINER_CLASS;
            }
            if ("screen_title".equals(normalized)) {
                return SCREEN_TITLE;
            }
            throw new IllegalArgumentException("Unsupported target_type for Minecraft 1.8.9: " + value);
        }
    }

    public final ResourceLocation source;
    public final TargetType targetType;
    public final String target;
    public final ClassMatchMode classMatch;
    public final int xOffset;
    public final int yOffset;
    public final int widthOffset;
    public final int heightOffset;
    public final int titleXOffset;
    public final int titleYOffset;
    public final int labelXOffset;
    public final int labelYOffset;
    public final Integer titleColor;
    public final Integer labelColor;
    public final boolean titleHidden;
    public final boolean labelHidden;
    public final List<SlotModifier> slotModifiers;
    public final List<SpriteOverlay> sprites;
    public final List<TextOverlay> texts;
    public final DefinitionCandidate nativeCandidate;
    public final ReactiveDefinition reactive;

    private GuiDefinition(
            ResourceLocation source,
            TargetType targetType,
            String target,
            ClassMatchMode classMatch,
            int xOffset,
            int yOffset,
            int widthOffset,
            int heightOffset,
            int titleXOffset,
            int titleYOffset,
            int labelXOffset,
            int labelYOffset,
            Integer titleColor,
            Integer labelColor,
            boolean titleHidden,
            boolean labelHidden,
            List<SlotModifier> slotModifiers,
            List<SpriteOverlay> sprites,
            List<TextOverlay> texts,
            DefinitionCandidate nativeCandidate,
            ReactiveDefinition reactive
    ) {
        this.source = source;
        this.targetType = targetType;
        this.target = target;
        this.classMatch = classMatch;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.widthOffset = widthOffset;
        this.heightOffset = heightOffset;
        this.titleXOffset = titleXOffset;
        this.titleYOffset = titleYOffset;
        this.labelXOffset = labelXOffset;
        this.labelYOffset = labelYOffset;
        this.titleColor = titleColor;
        this.labelColor = labelColor;
        this.titleHidden = titleHidden;
        this.labelHidden = labelHidden;
        this.slotModifiers = Collections.unmodifiableList(slotModifiers);
        this.sprites = Collections.unmodifiableList(sprites);
        this.texts = Collections.unmodifiableList(texts);
        this.nativeCandidate = nativeCandidate;
        this.reactive = reactive == null ? ReactiveDefinition.EMPTY : reactive;
    }

    static GuiDefinition parse(
            ResourceLocation source,
            JsonObject json,
            GuiTextureResolver textureResolver
    ) {
        String target = JsonSupport.string(json, "target", "").trim();
        if (target.isEmpty()) {
            throw new IllegalArgumentException("Missing non-empty target");
        }

        List<SlotModifier> slots = new ArrayList<SlotModifier>();
        for (JsonElement element : array(json, "slot_modifiers")) {
            slots.add(SlotModifier.parse(element.getAsJsonObject()));
        }

        List<SpriteOverlay> sprites = new ArrayList<SpriteOverlay>();
        for (JsonElement element : array(json, "sprites")) {
            sprites.add(SpriteOverlay.parse(element.getAsJsonObject(), textureResolver));
        }
        for (JsonElement element : array(json, "custom_textures")) {
            sprites.add(SpriteOverlay.parseCustomTexture(element.getAsJsonObject(), textureResolver));
        }

        List<TextOverlay> texts = new ArrayList<TextOverlay>();
        for (JsonElement element : array(json, "texts")) {
            texts.add(TextOverlay.parse(element.getAsJsonObject()));
        }

        return new GuiDefinition(
                source,
                TargetType.parse(JsonSupport.string(json, "target_type", "container_class")),
                target,
                ClassMatchMode.parse(JsonSupport.string(json, "class_match", "exact")),
                JsonSupport.integer(json, "x_offset", 0),
                JsonSupport.integer(json, "y_offset", 0),
                JsonSupport.integer(json, "width_offset", 0),
                JsonSupport.integer(json, "height_offset", 0),
                JsonSupport.integer(json, "title_x_offset", 0),
                JsonSupport.integer(json, "title_y_offset", 0),
                JsonSupport.integer(json, "label_x_offset", 0),
                JsonSupport.integer(json, "label_y_offset", 0),
                JsonSupport.color(json, "title_color"),
                JsonSupport.color(json, "label_color"),
                false,
                false,
                slots,
                sprites,
                texts,
                null,
                ReactiveDefinition.EMPTY
        );
    }

    public static GuiDefinition fromNative(
            DefinitionCandidate candidate,
            List<SpriteOverlay> sprites
    ) {
        redfoxexpand.core.GuiDefinition nativeDefinition = candidate.definition();
        redfoxexpand.core.GuiDefinition.Geometry geometry = nativeDefinition.geometry();
        List<SlotModifier> slots = new ArrayList<SlotModifier>();
        for (redfoxexpand.core.GuiDefinition.SlotRule rule : nativeDefinition.slotRules()) {
            slots.add(SlotModifier.fromNative(rule));
        }
        List<TextOverlay> texts = new ArrayList<TextOverlay>();
        for (redfoxexpand.core.GuiDefinition.TextOverlay text : nativeDefinition.texts()) {
            texts.add(TextOverlay.fromNative(text));
        }
        int titleX = 0, titleY = 0, labelX = 0, labelY = 0;
        Integer titleColor = null, labelColor = null;
        boolean titleHidden = false, labelHidden = false;
        for (redfoxexpand.core.GuiDefinition.TextRule rule : nativeDefinition.textRules()) {
            if (rule.selector() == redfoxexpand.core.GuiDefinition.TextSelector.TITLE) {
                titleX += rule.xOffset(); titleY += rule.yOffset();
                if (rule.color() != null) titleColor = rule.color();
                titleHidden |= rule.hidden();
            } else {
                labelX += rule.xOffset(); labelY += rule.yOffset();
                if (rule.color() != null) labelColor = rule.color();
                labelHidden |= rule.hidden();
            }
        }
        return new GuiDefinition(
                new ResourceLocation(candidate.sourcePath()),
                TargetType.CONTAINER_CLASS,
                "", ClassMatchMode.EXACT,
                geometry.xOffset(), geometry.yOffset(), geometry.widthOffset(), geometry.heightOffset(),
                titleX, titleY, labelX, labelY, titleColor, labelColor,
                titleHidden, labelHidden,
                slots, new ArrayList<SpriteOverlay>(sprites), texts,
                candidate, nativeDefinition.reactive()
        );
    }

    private static JsonArray array(JsonObject json, String key) {
        JsonElement value = json.get(key);
        return value == null || value.isJsonNull() ? new JsonArray() : value.getAsJsonArray();
    }
}
