package redfoxexpand.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Strict parser for the RedFoxExpand resource-pack API v2. */
public final class SchemaV2Parser {
    private static final Set<String> ROOT_FIELDS = fields("api_version", "definitions");
    private static final Set<String> DEFINITION_FIELDS = fields(
            "id", "operation", "priority", "match", "geometry",
            "slot_modifiers", "sprites", "texts", "text_rules"
    );
    private final boolean registryMatchersSupported;

    public SchemaV2Parser() { this(true); }

    public SchemaV2Parser(boolean registryMatchersSupported) {
        this.registryMatchersSupported = registryMatchersSupported;
    }

    public List<ParsedDefinition> parse(Reader reader, String source) {
        JsonElement root = new JsonParser().parse(reader);
        checkDepth(root, 0, source);
        JsonObject object = object(root, source + " root");
        rejectUnknown(object, ROOT_FIELDS, source + " root");
        int apiVersion = requiredInt(object, "api_version", source);
        if (apiVersion != 2) {
            throw failure(source, "api_version must be 2");
        }
        JsonArray definitions = requiredArray(object, "definitions", source);
        if (definitions.size() > ResourceLimits.MAX_DEFINITIONS_PER_FILE) {
            throw failure(source, "definitions exceeds " + ResourceLimits.MAX_DEFINITIONS_PER_FILE);
        }
        List<ParsedDefinition> result = new ArrayList<>();
        for (int index = 0; index < definitions.size(); index++) {
            result.add(parseDefinition(object(definitions.get(index), source + " definition " + index),
                    source, index));
        }
        return Collections.unmodifiableList(result);
    }

    private ParsedDefinition parseDefinition(JsonObject json, String source, int index) {
        String label = source + " definition " + index;
        rejectUnknown(json, DEFINITION_FIELDS, label);
        String id = requiredString(json, "id", label);
        if (id.length() > ResourceLimits.MAX_PATH_LENGTH || !id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw failure(label, "id must be a lowercase namespaced identifier");
        }
        DefinitionCandidate.Operation operation = parseEnum(
                string(json, "operation", "append", label),
                DefinitionCandidate.Operation.class, "operation", label
        );
        int priority = integer(json, "priority", 0, label);
        MatchSpec matcher = parseMatch(requiredObject(json, "match", label), label + " match");
        GuiDefinition.Geometry geometry = json.has("geometry")
                ? parseGeometry(requiredObject(json, "geometry", label), label + " geometry")
                : GuiDefinition.Geometry.ZERO;
        List<GuiDefinition.SlotRule> slots = parseSlotRules(json, label);
        List<GuiDefinition.Sprite> sprites = parseSprites(json, label);
        List<GuiDefinition.TextOverlay> texts = parseTexts(json, label);
        List<GuiDefinition.TextRule> textRules = parseTextRules(json, label);
        return new ParsedDefinition(id, operation, priority, matcher,
                new GuiDefinition(geometry, slots, sprites, texts, textRules));
    }

    private MatchSpec parseMatch(JsonObject json, String label) {
        if (json.entrySet().size() != 1) {
            throw failure(label, "matcher object must contain exactly one operator");
        }
        String key = keys(json).iterator().next();
        JsonElement value = json.get(key);
        if ("all".equals(key)) return new MatchSpec.All(parseTerms(value, label + ".all"));
        if ("any".equals(key)) return new MatchSpec.Any(parseTerms(value, label + ".any"));
        if ("not".equals(key)) return new MatchSpec.Not(parseMatch(object(value, label + ".not"), label + ".not"));
        if ("exact_screen_class".equals(key)) return new MatchSpec.ExactScreenClass(fullClass(value, label));
        if ("assignable_screen_class".equals(key)) return new MatchSpec.AssignableScreenClass(fullClass(value, label));
        if ("exact_screen_simple_class".equals(key)) return new MatchSpec.ExactScreenSimpleClass(simpleClass(value, label));
        if ("assignable_screen_simple_class".equals(key)) return new MatchSpec.AssignableScreenSimpleClass(simpleClass(value, label));
        if ("exact_menu_class".equals(key)) return new MatchSpec.ExactMenuClass(fullClass(value, label));
        if ("assignable_menu_class".equals(key)) return new MatchSpec.AssignableMenuClass(fullClass(value, label));
        if ("exact_menu_simple_class".equals(key)) return new MatchSpec.ExactMenuSimpleClass(simpleClass(value, label));
        if ("assignable_menu_simple_class".equals(key)) return new MatchSpec.AssignableMenuSimpleClass(simpleClass(value, label));
        if ("screen_title_key".equals(key)) return new MatchSpec.ScreenTitleKey(nonEmpty(value, label));
        if ("screen_title_text".equals(key)) return new MatchSpec.ScreenTitleText(nonEmpty(value, label));
        if ("menu_type".equals(key) || "resource_location".equals(key)
                || "mod_namespace".equals(key)) {
            if (!registryMatchersSupported) {
                throw failure(label, key + " is unavailable on the Minecraft 1.7.10 platform");
            }
            if ("menu_type".equals(key)) return new MatchSpec.MenuType(identifier(value, label));
            if ("resource_location".equals(key)) return new MatchSpec.ResourceLocation(identifier(value, label));
            return new MatchSpec.ModNamespace(namespace(value, label));
        }
        throw failure(label, "unknown matcher operator: " + key);
    }

    private List<MatchSpec> parseTerms(JsonElement element, String label) {
        JsonArray array = array(element, label);
        if (array.size() == 0 || array.size() > ResourceLimits.MAX_DEFINITIONS_PER_FILE) {
            throw failure(label, "must contain 1.." + ResourceLimits.MAX_DEFINITIONS_PER_FILE + " terms");
        }
        List<MatchSpec> terms = new ArrayList<>();
        for (int index = 0; index < array.size(); index++) {
            terms.add(parseMatch(object(array.get(index), label + "[" + index + "]"),
                    label + "[" + index + "]"));
        }
        return terms;
    }

    private GuiDefinition.Geometry parseGeometry(JsonObject json, String label) {
        rejectUnknown(json, fields("x_offset", "y_offset", "width_offset", "height_offset"), label);
        return new GuiDefinition.Geometry(
                integer(json, "x_offset", 0, label), integer(json, "y_offset", 0, label),
                integer(json, "width_offset", 0, label), integer(json, "height_offset", 0, label)
        );
    }

    private List<GuiDefinition.SlotRule> parseSlotRules(JsonObject root, String label) {
        JsonArray array = optionalArray(root, "slot_modifiers", label);
        checkCount(array, ResourceLimits.MAX_SLOT_RULES_PER_DEFINITION, label + ".slot_modifiers");
        List<GuiDefinition.SlotRule> result = new ArrayList<>();
        Set<String> fields = fields("slots", "target_x", "target_y", "target_class_name",
                "target_class_match", "target_class_name_type", "x_offset", "y_offset",
                "highlight_color", "highlight_color_2");
        for (int index = 0; index < array.size(); index++) {
            String itemLabel = label + ".slot_modifiers[" + index + "]";
            JsonObject json = object(array.get(index), itemLabel);
            rejectUnknown(json, fields, itemLabel);
            Set<Integer> slots = parseSlots(json.get("slots"), itemLabel);
            String targetClass = nullableString(json, "target_class_name", itemLabel);
            GuiDefinition.ClassMatchMode mode = parseEnum(
                    string(json, "target_class_match", "exact", itemLabel),
                    GuiDefinition.ClassMatchMode.class, "target_class_match", itemLabel
            );
            GuiDefinition.ClassNameType nameType = parseEnum(
                    string(json, "target_class_name_type", "full", itemLabel),
                    GuiDefinition.ClassNameType.class, "target_class_name_type", itemLabel
            );
            if (targetClass != null) {
                targetClass = targetClass.trim();
                if (targetClass.isEmpty()) throw failure(itemLabel, "target_class_name must not be empty");
                if (nameType == GuiDefinition.ClassNameType.FULL && !targetClass.contains(".")) {
                    throw failure(itemLabel, "full target_class_name must be fully qualified; use name_type simple explicitly");
                }
                if (nameType == GuiDefinition.ClassNameType.SIMPLE && targetClass.contains(".")) {
                    throw failure(itemLabel, "simple target_class_name must not be qualified");
                }
            }
            result.add(new GuiDefinition.SlotRule(slots,
                    optionalInt(json, "target_x", itemLabel), optionalInt(json, "target_y", itemLabel),
                    targetClass, mode, nameType,
                    integer(json, "x_offset", 0, itemLabel), integer(json, "y_offset", 0, itemLabel),
                    optionalColor(json, "highlight_color", itemLabel),
                    optionalColor(json, "highlight_color_2", itemLabel)));
        }
        return result;
    }

    private Set<Integer> parseSlots(JsonElement element, String label) {
        if (element == null || element.isJsonNull()) return Collections.emptySet();
        JsonArray values = element.isJsonArray() ? element.getAsJsonArray() : singleton(element);
        Set<Integer> result = new LinkedHashSet<>();
        for (JsonElement value : values) {
            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                int slot = ResourceLimits.boundedInt(value.getAsDouble(), label + ".slots");
                if (slot < 0) throw failure(label, "slot indices must be non-negative");
                result.add(slot);
                continue;
            }
            String expression = nonEmpty(value, label + ".slots");
            int separator = expression.indexOf('-');
            if (separator < 0) {
                int slot = parseNonNegative(expression, label + ".slots");
                result.add(slot);
                continue;
            }
            int start = parseNonNegative(expression.substring(0, separator).trim(), label + ".slots");
            int end = parseNonNegative(expression.substring(separator + 1).trim(), label + ".slots");
            if (Math.abs((long) end - start) + 1 > ResourceLimits.MAX_SLOT_RANGE) {
                throw failure(label, "slot range exceeds " + ResourceLimits.MAX_SLOT_RANGE);
            }
            int direction = start <= end ? 1 : -1;
            for (int slot = start; ; slot += direction) {
                result.add(slot);
                if (slot == end) break;
            }
        }
        return result;
    }

    private List<GuiDefinition.Sprite> parseSprites(JsonObject root, String label) {
        JsonArray array = optionalArray(root, "sprites", label);
        checkCount(array, ResourceLimits.MAX_SPRITES_PER_DEFINITION, label + ".sprites");
        List<GuiDefinition.Sprite> result = new ArrayList<>();
        Set<String> fields = fields("texture", "animation", "x", "y", "z", "u", "v",
                "width", "height", "source_width", "source_height", "texture_width",
                "texture_height", "full_texture", "color", "layer", "anchor");
        for (int index = 0; index < array.size(); index++) {
            String itemLabel = label + ".sprites[" + index + "]";
            JsonObject json = object(array.get(index), itemLabel);
            rejectUnknown(json, fields, itemLabel);
            GuiDefinition.TextureSpec texture = parseTexture(requiredObject(json, "texture", itemLabel), itemLabel + ".texture");
            double width = number(json, "width", 16.0, itemLabel);
            double height = number(json, "height", 16.0, itemLabel);
            double sourceWidth = number(json, "source_width", width, itemLabel);
            double sourceHeight = number(json, "source_height", height, itemLabel);
            double textureWidth = number(json, "texture_width", 256.0, itemLabel);
            double textureHeight = number(json, "texture_height", 256.0, itemLabel);
            if (width <= 0 || height <= 0 || sourceWidth <= 0 || sourceHeight <= 0
                    || textureWidth <= 0 || textureHeight <= 0) {
                throw failure(itemLabel, "sprite dimensions must be positive");
            }
            GuiDefinition.Animation animation = json.has("animation")
                    ? parseAnimation(requiredObject(json, "animation", itemLabel), texture, itemLabel + ".animation")
                    : null;
            result.add(new GuiDefinition.Sprite(texture, animation,
                    number(json, "x", 0.0, itemLabel), number(json, "y", 0.0, itemLabel),
                    number(json, "z", 0.0, itemLabel), number(json, "u", 0.0, itemLabel),
                    number(json, "v", 0.0, itemLabel), width, height, sourceWidth, sourceHeight,
                    textureWidth, textureHeight, bool(json, "full_texture", true, itemLabel),
                    color(json, "color", 0xFFFFFFFF, itemLabel),
                    parseEnum(string(json, "layer", "background", itemLabel), GuiDefinition.Layer.class, "layer", itemLabel),
                    parseEnum(string(json, "anchor", "gui", itemLabel), GuiDefinition.Anchor.class, "anchor", itemLabel)));
        }
        return result;
    }

    private GuiDefinition.Animation parseAnimation(JsonObject json, GuiDefinition.TextureSpec fallback, String label) {
        rejectUnknown(json, fields("frames", "frame_duration_ms", "loop", "condition",
                "default_texture", "missing_frame"), label);
        int defaultDuration = positiveDuration(integer(json, "frame_duration_ms", 100, label), label);
        JsonArray frames = requiredArray(json, "frames", label);
        if (frames.size() == 0 || frames.size() > ResourceLimits.MAX_ANIMATION_FRAMES) {
            throw failure(label, "frames must contain 1.." + ResourceLimits.MAX_ANIMATION_FRAMES + " entries");
        }
        List<GuiDefinition.AnimationFrame> parsed = new ArrayList<>();
        for (int index = 0; index < frames.size(); index++) {
            String frameLabel = label + ".frames[" + index + "]";
            JsonObject frame = object(frames.get(index), frameLabel);
            rejectUnknown(frame, fields("texture", "duration_ms"), frameLabel);
            parsed.add(new GuiDefinition.AnimationFrame(
                    parseTexture(requiredObject(frame, "texture", frameLabel), frameLabel + ".texture"),
                    positiveDuration(integer(frame, "duration_ms", defaultDuration, frameLabel), frameLabel)));
        }
        GuiDefinition.TextureSpec defaultTexture = json.has("default_texture")
                ? parseTexture(requiredObject(json, "default_texture", label), label + ".default_texture")
                : fallback;
        return new GuiDefinition.Animation(parsed, bool(json, "loop", true, label),
                parseEnum(string(json, "condition", "always", label), GuiDefinition.AnimationCondition.class, "condition", label),
                defaultTexture,
                parseEnum(string(json, "missing_frame", "use_default", label), GuiDefinition.MissingFrameBehavior.class, "missing_frame", label));
    }

    private GuiDefinition.TextureSpec parseTexture(JsonObject json, String label) {
        rejectUnknown(json, fields("type", "location"), label);
        GuiDefinition.ResourceType type = parseEnum(requiredString(json, "type", label),
                GuiDefinition.ResourceType.class, "type", label);
        String location = ResourceLimits.safePath(requiredString(json, "location", label), label + ".location");
        if (type != GuiDefinition.ResourceType.PACK_RESOURCE && !location.contains(":")) {
            throw failure(label, type.name().toLowerCase(Locale.ROOT) + " requires a namespaced location");
        }
        return new GuiDefinition.TextureSpec(type, location);
    }

    private List<GuiDefinition.TextOverlay> parseTexts(JsonObject root, String label) {
        JsonArray array = optionalArray(root, "texts", label);
        checkCount(array, ResourceLimits.MAX_TEXTS_PER_DEFINITION, label + ".texts");
        List<GuiDefinition.TextOverlay> result = new ArrayList<>();
        Set<String> fields = fields("text", "translate", "x", "y", "color", "shadow", "layer", "anchor");
        for (int index = 0; index < array.size(); index++) {
            String itemLabel = label + ".texts[" + index + "]";
            JsonObject json = object(array.get(index), itemLabel);
            rejectUnknown(json, fields, itemLabel);
            result.add(new GuiDefinition.TextOverlay(requiredString(json, "text", itemLabel),
                    bool(json, "translate", false, itemLabel),
                    integer(json, "x", 0, itemLabel), integer(json, "y", 0, itemLabel),
                    color(json, "color", 0xFFFFFFFF, itemLabel), bool(json, "shadow", false, itemLabel),
                    parseEnum(string(json, "layer", "foreground", itemLabel), GuiDefinition.Layer.class, "layer", itemLabel),
                    parseEnum(string(json, "anchor", "gui", itemLabel), GuiDefinition.Anchor.class, "anchor", itemLabel)));
        }
        return result;
    }

    private List<GuiDefinition.TextRule> parseTextRules(JsonObject root, String label) {
        JsonArray array = optionalArray(root, "text_rules", label);
        checkCount(array, ResourceLimits.MAX_TEXT_RULES_PER_DEFINITION, label + ".text_rules");
        List<GuiDefinition.TextRule> result = new ArrayList<>();
        for (int index = 0; index < array.size(); index++) {
            String itemLabel = label + ".text_rules[" + index + "]";
            JsonObject json = object(array.get(index), itemLabel);
            rejectUnknown(json, fields("selector", "x_offset", "y_offset", "color", "hidden"), itemLabel);
            result.add(new GuiDefinition.TextRule(
                    parseEnum(requiredString(json, "selector", itemLabel), GuiDefinition.TextSelector.class, "selector", itemLabel),
                    integer(json, "x_offset", 0, itemLabel), integer(json, "y_offset", 0, itemLabel),
                    optionalColor(json, "color", itemLabel), bool(json, "hidden", false, itemLabel)));
        }
        return result;
    }

    private static <E extends Enum<E>> E parseEnum(String value, Class<E> type, String field, String label) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException error) {
            throw failure(label, "invalid " + field + ": " + value);
        }
    }

    private static void rejectUnknown(JsonObject json, Set<String> allowed, String label) {
        Set<String> unknown = new HashSet<String>(keys(json));
        unknown.removeAll(allowed);
        if (!unknown.isEmpty()) throw failure(label, "unknown field(s): " + unknown);
    }

    private static JsonObject requiredObject(JsonObject json, String field, String label) {
        JsonElement value = json.get(field);
        if (value == null) throw failure(label, "missing required object: " + field);
        return object(value, label + "." + field);
    }

    private static JsonArray requiredArray(JsonObject json, String field, String label) {
        JsonElement value = json.get(field);
        if (value == null) throw failure(label, "missing required array: " + field);
        return array(value, label + "." + field);
    }

    private static JsonArray optionalArray(JsonObject json, String field, String label) {
        JsonElement value = json.get(field);
        return value == null || value.isJsonNull() ? new JsonArray() : array(value, label + "." + field);
    }

    private static JsonObject object(JsonElement value, String label) {
        if (value == null || !value.isJsonObject()) throw failure(label, "must be an object");
        return value.getAsJsonObject();
    }

    private static JsonArray array(JsonElement value, String label) {
        if (value == null || !value.isJsonArray()) throw failure(label, "must be an array");
        return value.getAsJsonArray();
    }

    private static JsonArray singleton(JsonElement value) {
        JsonArray array = new JsonArray();
        array.add(value);
        return array;
    }

    private static String requiredString(JsonObject json, String field, String label) {
        JsonElement value = json.get(field);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw failure(label, "missing or non-string field: " + field);
        }
        return value.getAsString();
    }

    private static String nullableString(JsonObject json, String field, String label) {
        if (!json.has(field) || json.get(field).isJsonNull()) return null;
        return requiredString(json, field, label);
    }

    private static String string(JsonObject json, String field, String fallback, String label) {
        return json.has(field) ? requiredString(json, field, label) : fallback;
    }

    private static int requiredInt(JsonObject json, String field, String label) {
        if (!json.has(field)) throw failure(label, "missing required integer: " + field);
        return integer(json, field, 0, label);
    }

    private static int integer(JsonObject json, String field, int fallback, String label) {
        if (!json.has(field)) return fallback;
        JsonElement value = json.get(field);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw failure(label, field + " must be a number");
        }
        return ResourceLimits.boundedInt(value.getAsDouble(), label + "." + field);
    }

    private static Integer optionalInt(JsonObject json, String field, String label) {
        return json.has(field) ? integer(json, field, 0, label) : null;
    }

    private static double number(JsonObject json, String field, double fallback, String label) {
        if (!json.has(field)) return fallback;
        JsonElement value = json.get(field);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw failure(label, field + " must be a number");
        }
        return ResourceLimits.finiteGui(value.getAsDouble(), label + "." + field);
    }

    private static boolean bool(JsonObject json, String field, boolean fallback, String label) {
        if (!json.has(field)) return fallback;
        JsonElement value = json.get(field);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
            throw failure(label, field + " must be a boolean");
        }
        return value.getAsBoolean();
    }

    private static Integer optionalColor(JsonObject json, String field, String label) {
        return json.has(field) ? color(json, field, 0, label) : null;
    }

    private static int color(JsonObject json, String field, int fallback, String label) {
        if (!json.has(field)) return fallback;
        JsonElement value = json.get(field);
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) return value.getAsInt();
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw failure(label, field + " must be an integer or #RRGGBB/#AARRGGBB");
        }
        String raw = value.getAsString().trim().replace("_", "");
        if (raw.startsWith("#")) raw = raw.substring(1);
        else if (raw.toLowerCase(Locale.ROOT).startsWith("0x")) raw = raw.substring(2);
        if (raw.length() == 6) raw = "FF" + raw;
        if (raw.length() != 8) throw failure(label, field + " must be #RRGGBB or #AARRGGBB");
        try {
            return (int) Long.parseLong(raw, 16);
        } catch (NumberFormatException error) {
            throw failure(label, "invalid color in " + field);
        }
    }

    private static String nonEmpty(JsonElement value, String label) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw failure(label, "must be a string");
        }
        String result = value.getAsString().trim();
        if (result.isEmpty()) throw failure(label, "must not be empty");
        return result;
    }

    private static String fullClass(JsonElement value, String label) {
        String result = nonEmpty(value, label);
        if (!result.contains(".")) throw failure(label, "full class matcher requires a qualified class name");
        return result;
    }

    private static String simpleClass(JsonElement value, String label) {
        String result = nonEmpty(value, label);
        if (result.contains(".")) throw failure(label, "simple class matcher must not be qualified");
        return result;
    }

    private static String identifier(JsonElement value, String label) {
        String result = nonEmpty(value, label).toLowerCase(Locale.ROOT);
        if (!result.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw failure(label, "must be a lowercase namespaced identifier");
        }
        return result;
    }

    private static String namespace(JsonElement value, String label) {
        String result = nonEmpty(value, label).toLowerCase(Locale.ROOT);
        if (!result.matches("[a-z0-9_.-]+")) throw failure(label, "must be a lowercase namespace");
        return result;
    }

    private static int parseNonNegative(String value, String label) {
        try {
            int result = Integer.parseInt(value);
            if (result < 0) throw failure(label, "must be non-negative");
            return result;
        } catch (NumberFormatException error) {
            throw failure(label, "invalid integer: " + value);
        }
    }

    private static int positiveDuration(int value, String label) {
        if (value <= 0 || value > 600_000) throw failure(label, "duration must be 1..600000 ms");
        return value;
    }

    private static void checkCount(JsonArray array, int maximum, String label) {
        if (array.size() > maximum) throw failure(label, "exceeds " + maximum + " entries");
    }

    private static void checkDepth(JsonElement value, int depth, String label) {
        if (depth > ResourceLimits.MAX_JSON_NESTING) {
            throw failure(label, "JSON nesting exceeds " + ResourceLimits.MAX_JSON_NESTING);
        }
        if (value.isJsonArray()) {
            for (JsonElement child : value.getAsJsonArray()) checkDepth(child, depth + 1, label);
        } else if (value.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : value.getAsJsonObject().entrySet()) {
                checkDepth(entry.getValue(), depth + 1, label);
            }
        }
    }

    private static IllegalArgumentException failure(String label, String message) {
        return new IllegalArgumentException(label + ": " + message);
    }

    private static Set<String> fields(String... values) {
        return new HashSet<String>(Arrays.asList(values));
    }

    private static Set<String> keys(JsonObject json) {
        Set<String> result = new HashSet<String>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) result.add(entry.getKey());
        return result;
    }

    public static final class ParsedDefinition {
        private final String id;
        private final DefinitionCandidate.Operation operation;
        private final int priority;
        private final MatchSpec matcher;
        private final GuiDefinition definition;
        public ParsedDefinition(String id, DefinitionCandidate.Operation operation, int priority,
                                MatchSpec matcher, GuiDefinition definition) {
            this.id = id; this.operation = operation; this.priority = priority;
            this.matcher = matcher; this.definition = definition;
        }
        public String id() { return id; }
        public DefinitionCandidate.Operation operation() { return operation; }
        public int priority() { return priority; }
        public MatchSpec matcher() { return matcher; }
        public GuiDefinition definition() { return definition; }
    }
}
