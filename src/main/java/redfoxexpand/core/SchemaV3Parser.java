package redfoxexpand.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import redfoxexpand.reactive.ReactiveDefinition;
import redfoxexpand.reactive.ReactiveLimits;
import redfoxexpand.reactive.animation.Interpolation;
import redfoxexpand.reactive.animation.CompositionMode;
import redfoxexpand.reactive.animation.PropertyAnimation;
import redfoxexpand.reactive.animation.PropertyKeyframe;
import redfoxexpand.reactive.animation.PropertyTrack;
import redfoxexpand.reactive.behavior.Action;
import redfoxexpand.reactive.behavior.BehaviorRule;
import redfoxexpand.reactive.behavior.EventTrigger;
import redfoxexpand.reactive.behavior.PlayAnimationAction;
import redfoxexpand.reactive.behavior.SetAlphaAction;
import redfoxexpand.reactive.behavior.SetVisibleAction;
import redfoxexpand.reactive.behavior.StopAnimationAction;
import redfoxexpand.reactive.binding.Binding;
import redfoxexpand.reactive.binding.ReactiveProperty;
import redfoxexpand.reactive.event.RuntimeEvent;
import redfoxexpand.reactive.expression.CompiledExpression;
import redfoxexpand.reactive.expression.ExpressionCompiler;
import redfoxexpand.reactive.expression.DerivedValue;
import redfoxexpand.reactive.runtime.Capability;
import redfoxexpand.reactive.runtime.RuntimeVariables;
import redfoxexpand.reactive.value.ValueType;
import redfoxexpand.reactive.value.RuntimeValue;

import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Strict additive Schema v3 parser. The v2 parser remains the sole parser for base Definition fields. */
public final class SchemaV3Parser {
    private static final Set<String> ROOT_FIELDS = fields("api_version", "definitions");
    private static final Set<String> DEFINITION_FIELDS = fields(
            "id", "operation", "priority", "match", "geometry", "slot_modifiers",
            "sprites", "texts", "text_rules", "bindings", "animations", "behaviors");
    private static final Set<String> SPRITE_FIELDS = fields(
            "id", "texture", "animation", "x", "y", "z", "u", "v", "width", "height",
            "source_width", "source_height", "texture_width", "texture_height", "full_texture",
            "color", "layer", "anchor");
    private static final Set<String> V31_DEFINITION_FIELDS = fields(
            "id", "operation", "priority", "match", "geometry", "slot_modifiers",
            "elements", "texts", "text_rules", "constants", "values",
            "bindings", "animations", "behaviors");
    private static final Set<String> V31_SPRITE_FIELDS = fields(
            "id", "type", "texture", "animation", "x", "y", "z", "u", "v",
            "width", "height", "source_width", "source_height", "texture_width",
            "texture_height", "full_texture", "color", "layer", "anchor", "pivot");
    private static final Set<String> V31_GROUP_FIELDS = fields(
            "id", "type", "children", "x", "y", "width", "height", "anchor", "pivot");
    private static final Set<String> EVENTS = fields(
            RuntimeEvent.HEALTH_DECREASED, RuntimeEvent.HEALTH_INCREASED,
            RuntimeEvent.STARTED_BURNING, RuntimeEvent.STOPPED_BURNING,
            RuntimeEvent.SCREEN_OPENED);

    private final Set<Capability> capabilities;
    private final SchemaV2Parser baseParser = new SchemaV2Parser();
    private final ExpressionCompiler stateExpressions;
    private final ExpressionCompiler healthEventExpressions;
    private final ExpressionCompiler stateEventExpressions;

    public SchemaV3Parser(Set<Capability> capabilities) {
        this.capabilities = capabilities.isEmpty()
                ? Collections.<Capability>emptySet() : EnumSet.copyOf(capabilities);
        Set<String> unsupportedState = unsupportedVariables(RuntimeVariables.stateTypes().keySet());
        Set<String> unsupportedHealth = new HashSet<String>(unsupportedState);
        if (!capabilities.contains(Capability.EVENT_HEALTH)) {
            unsupportedHealth.add("event.old");
            unsupportedHealth.add("event.new");
            unsupportedHealth.add("event.delta");
        }
        stateExpressions = new ExpressionCompiler(RuntimeVariables.stateTypes(), unsupportedState);
        healthEventExpressions = new ExpressionCompiler(RuntimeVariables.eventTypes(), unsupportedHealth);
        stateEventExpressions = new ExpressionCompiler(RuntimeVariables.stateTypes(), unsupportedState);
    }

    public List<SchemaV2Parser.ParsedDefinition> parse(Reader reader, String source) {
        JsonElement parsed = JsonParser.parseReader(reader);
        checkDepth(parsed, 0, source);
        JsonObject root = object(parsed, source + " root");
        rejectUnknown(root, ROOT_FIELDS, source + " root");
        int apiVersion = requiredApiVersion(root, source);
        if (apiVersion == 31) return parseV31(root, source);
        if (apiVersion != 3) throw failure(source, "api_version must be 3 or 3.1");
        JsonArray definitions = requiredArray(root, "definitions", source);
        if (definitions.size() > ResourceLimits.MAX_DEFINITIONS_PER_FILE) {
            throw failure(source, "definitions exceeds " + ResourceLimits.MAX_DEFINITIONS_PER_FILE);
        }

        JsonObject baseRoot = root.deepCopy();
        baseRoot.addProperty("api_version", Integer.valueOf(2));
        JsonArray baseDefinitions = baseRoot.getAsJsonArray("definitions");
        for (int index = 0; index < definitions.size(); index++) {
            JsonObject definition = object(definitions.get(index), label(source, index));
            rejectUnknown(definition, DEFINITION_FIELDS, label(source, index));
            JsonObject baseDefinition = object(baseDefinitions.get(index), label(source, index));
            baseDefinition.remove("bindings");
            baseDefinition.remove("animations");
            baseDefinition.remove("behaviors");
            JsonArray sprites = optionalArray(definition, "sprites", label(source, index));
            JsonArray baseSprites = optionalArray(baseDefinition, "sprites", label(source, index));
            for (int spriteIndex = 0; spriteIndex < sprites.size(); spriteIndex++) {
                JsonObject sprite = object(sprites.get(spriteIndex), label(source, index) + ".sprites[" + spriteIndex + "]");
                rejectUnknown(sprite, SPRITE_FIELDS, label(source, index) + ".sprites[" + spriteIndex + "]");
                object(baseSprites.get(spriteIndex), label(source, index) + ".sprites[" + spriteIndex + "]").remove("id");
            }
        }

        List<SchemaV2Parser.ParsedDefinition> bases = baseParser.parse(
                new StringReader(baseRoot.toString()), source + " [v3 base]");
        List<SchemaV2Parser.ParsedDefinition> result = new ArrayList<SchemaV2Parser.ParsedDefinition>();
        for (int index = 0; index < definitions.size(); index++) {
            JsonObject json = definitions.get(index).getAsJsonObject();
            SchemaV2Parser.ParsedDefinition base = bases.get(index);
            ParsedElements elements = attachElementIds(json, base.definition().sprites(), label(source, index));
            ReactiveDefinition reactive = parseReactive(json, elements.ids, label(source, index));
            GuiDefinition original = base.definition();
            GuiDefinition definition = new GuiDefinition(original.geometry(), original.slotRules(),
                    elements.sprites, original.texts(), original.textRules(), reactive);
            result.add(new SchemaV2Parser.ParsedDefinition(base.id(), base.operation(), base.priority(),
                    base.matcher(), definition));
        }
        return Collections.unmodifiableList(result);
    }

    private List<SchemaV2Parser.ParsedDefinition> parseV31(JsonObject root, String source) {
        JsonArray definitions = requiredArray(root, "definitions", source);
        if (definitions.size() > ResourceLimits.MAX_DEFINITIONS_PER_FILE) {
            throw failure(source, "definitions exceeds " + ResourceLimits.MAX_DEFINITIONS_PER_FILE);
        }

        JsonObject baseRoot = new JsonObject();
        baseRoot.addProperty("api_version", Integer.valueOf(2));
        JsonArray baseDefinitions = new JsonArray();
        baseRoot.add("definitions", baseDefinitions);
        for (int index = 0; index < definitions.size(); index++) {
            String definitionLabel = label(source, index);
            JsonObject definition = object(definitions.get(index), definitionLabel);
            rejectUnknown(definition, V31_DEFINITION_FIELDS, definitionLabel);
            JsonArray elements = optionalArray(definition, "elements", definitionLabel);
            if (elements.size() > ResourceLimits.MAX_ELEMENTS_PER_DEFINITION) {
                throw failure(definitionLabel, "elements exceeds "
                        + ResourceLimits.MAX_ELEMENTS_PER_DEFINITION);
            }
            JsonObject base = definition.deepCopy();
            base.remove("elements");
            base.remove("constants");
            base.remove("values");
            base.remove("bindings");
            base.remove("animations");
            base.remove("behaviors");
            JsonArray sprites = new JsonArray();
            for (int elementIndex = 0; elementIndex < elements.size(); elementIndex++) {
                String elementLabel = definitionLabel + ".elements[" + elementIndex + "]";
                JsonObject element = object(elements.get(elementIndex), elementLabel);
                String type = requiredString(element, "type", elementLabel);
                if ("sprite".equals(type)) {
                    rejectUnknown(element, V31_SPRITE_FIELDS, elementLabel);
                    JsonObject sprite = element.deepCopy();
                    sprite.remove("id");
                    sprite.remove("type");
                    sprite.remove("pivot");
                    if (sprite.has("anchor")) {
                        sprite.addProperty("anchor", compatibleBaseAnchor(
                                requiredString(sprite, "anchor", elementLabel), elementLabel));
                    }
                    sprites.add(sprite);
                } else if ("group".equals(type)) {
                    rejectUnknown(element, V31_GROUP_FIELDS, elementLabel);
                } else {
                    throw failure(elementLabel, "unknown element type: " + type);
                }
            }
            base.add("sprites", sprites);
            baseDefinitions.add(base);
        }

        List<SchemaV2Parser.ParsedDefinition> bases = baseParser.parse(
                new StringReader(baseRoot.toString()), source + " [v3.1 base]");
        List<SchemaV2Parser.ParsedDefinition> result = new ArrayList<SchemaV2Parser.ParsedDefinition>();
        for (int index = 0; index < definitions.size(); index++) {
            String definitionLabel = label(source, index);
            JsonObject json = definitions.get(index).getAsJsonObject();
            SchemaV2Parser.ParsedDefinition base = bases.get(index);
            ParsedScene scene = parseScene(json, base.definition().sprites(), definitionLabel);
            Symbols symbols = parseSymbols(json, definitionLabel);
            Map<String, ValueType> bindingTypes = new LinkedHashMap<String, ValueType>(symbols.types);
            bindingTypes.putAll(elementVariableTypes());
            Set<String> unsupportedState = unsupportedVariables(RuntimeVariables.stateTypes().keySet());
            ExpressionCompiler bindingCompiler = new ExpressionCompiler(bindingTypes, unsupportedState);
            ExpressionCompiler stateCompiler = new ExpressionCompiler(symbols.types, unsupportedState);
            Map<String, ValueType> healthTypes = new LinkedHashMap<String, ValueType>(symbols.types);
            healthTypes.put("event.old", ValueType.NUMBER);
            healthTypes.put("event.new", ValueType.NUMBER);
            healthTypes.put("event.delta", ValueType.NUMBER);
            Set<String> unsupportedHealth = new HashSet<String>(unsupportedState);
            if (!capabilities.contains(Capability.EVENT_HEALTH)) {
                unsupportedHealth.add("event.old");
                unsupportedHealth.add("event.new");
                unsupportedHealth.add("event.delta");
            }
            ExpressionCompiler healthCompiler = new ExpressionCompiler(healthTypes, unsupportedHealth);

            List<Binding> bindings = parseBindings(json, scene.ids, definitionLabel, bindingCompiler,
                    scene.parents);
            Map<String, PropertyAnimation> animations = parseAnimations(json, definitionLabel, true);
            List<BehaviorRule> behaviors = parseBehaviors(json, scene.ids, animations, definitionLabel,
                    healthCompiler, stateCompiler);
            ReactiveDefinition reactive = new ReactiveDefinition(bindings, animations, behaviors,
                    symbols.constants, symbols.values);
            GuiDefinition original = base.definition();
            GuiDefinition definition = new GuiDefinition(original.geometry(), original.slotRules(),
                    scene.sprites, original.texts(), original.textRules(), reactive, scene.groups);
            result.add(new SchemaV2Parser.ParsedDefinition(base.id(), base.operation(), base.priority(),
                    base.matcher(), definition));
        }
        return Collections.unmodifiableList(result);
    }

    private ParsedScene parseScene(JsonObject definition, List<GuiDefinition.Sprite> parsedSprites,
                                   String label) {
        JsonArray elements = optionalArray(definition, "elements", label);
        Map<String, JsonObject> byId = new LinkedHashMap<String, JsonObject>();
        Map<String, String> types = new LinkedHashMap<String, String>();
        Map<String, Integer> declarationOrder = new LinkedHashMap<String, Integer>();
        int groupCount = 0;
        for (int index = 0; index < elements.size(); index++) {
            String itemLabel = label + ".elements[" + index + "]";
            JsonObject element = object(elements.get(index), itemLabel);
            String id = stableId(requiredString(element, "id", itemLabel), itemLabel + ".id");
            if (byId.put(id, element) != null) throw failure(itemLabel, "duplicate element id: " + id);
            String type = requiredString(element, "type", itemLabel);
            types.put(id, type);
            declarationOrder.put(id, Integer.valueOf(index));
            if ("group".equals(type)) groupCount++;
        }
        if (groupCount > ResourceLimits.MAX_GROUPS_PER_DEFINITION) {
            throw failure(label, "groups exceeds " + ResourceLimits.MAX_GROUPS_PER_DEFINITION);
        }

        Map<String, String> parents = new LinkedHashMap<String, String>();
        Map<String, List<String>> children = new LinkedHashMap<String, List<String>>();
        for (Map.Entry<String, JsonObject> entry : byId.entrySet()) {
            if (!"group".equals(types.get(entry.getKey()))) continue;
            JsonArray configured = optionalArray(entry.getValue(), "children", label + ".elements["
                    + declarationOrder.get(entry.getKey()) + "]");
            if (configured.size() > ResourceLimits.MAX_CHILDREN_PER_GROUP) {
                throw failure(label, "group " + entry.getKey() + " children exceeds "
                        + ResourceLimits.MAX_CHILDREN_PER_GROUP);
            }
            List<String> list = new ArrayList<String>();
            Set<String> unique = new LinkedHashSet<String>();
            for (int childIndex = 0; childIndex < configured.size(); childIndex++) {
                JsonElement childValue = configured.get(childIndex);
                if (!childValue.isJsonPrimitive() || !childValue.getAsJsonPrimitive().isString()) {
                    throw failure(label, "group " + entry.getKey() + " child must be an element ID");
                }
                String child = stableId(childValue.getAsString(), label + ".group " + entry.getKey());
                if (!byId.containsKey(child)) {
                    throw failure(label, "group " + entry.getKey() + " target does not exist: " + child);
                }
                if (!unique.add(child)) throw failure(label, "group " + entry.getKey()
                        + " repeats child: " + child);
                String previous = parents.put(child, entry.getKey());
                if (previous != null) throw failure(label, "element " + child
                        + " has multiple parents: " + previous + " and " + entry.getKey());
                list.add(child);
            }
            children.put(entry.getKey(), Collections.unmodifiableList(list));
        }

        Map<String, Integer> sceneOrder = new LinkedHashMap<String, Integer>();
        Set<String> visiting = new LinkedHashSet<String>();
        Set<String> visited = new LinkedHashSet<String>();
        int[] nextOrder = {0};
        for (String id : byId.keySet()) {
            if (!parents.containsKey(id)) visitScene(id, children, visiting, visited,
                    sceneOrder, nextOrder, 1, label);
        }
        if (visited.size() != byId.size()) {
            for (String id : byId.keySet()) {
                if (!visited.contains(id)) visitScene(id, children, visiting, visited,
                        sceneOrder, nextOrder, 1, label);
            }
        }

        List<GuiDefinition.Sprite> sprites = new ArrayList<GuiDefinition.Sprite>();
        List<GuiDefinition.Group> groups = new ArrayList<GuiDefinition.Group>();
        int parsedIndex = 0;
        for (Map.Entry<String, JsonObject> entry : byId.entrySet()) {
            String id = entry.getKey();
            JsonObject element = entry.getValue();
            String itemLabel = label + ".elements[" + declarationOrder.get(id) + "]";
            String parent = parents.get(id);
            GuiDefinition.Anchor anchor = sceneAnchor(element, parent, itemLabel);
            int order = sceneOrder.get(id).intValue();
            if ("sprite".equals(types.get(id))) {
                GuiDefinition.Sprite parsed = parsedSprites.get(parsedIndex++);
                GuiDefinition.Pivot pivot = parsePivot(element, parsed.width(), parsed.height(), itemLabel);
                sprites.add(new GuiDefinition.Sprite(parsed.texture(), parsed.animation(), parsed.x(), parsed.y(),
                        parsed.z(), parsed.u(), parsed.v(), parsed.width(), parsed.height(), parsed.sourceWidth(),
                        parsed.sourceHeight(), parsed.textureWidth(), parsed.textureHeight(), parsed.fullTexture(),
                        parsed.color(), parsed.layer(), anchor, id, parent, pivot, order, true));
            } else {
                double width = optionalFiniteNumber(element, "width", 0.0D, itemLabel);
                double height = optionalFiniteNumber(element, "height", 0.0D, itemLabel);
                if (width < 0.0D || height < 0.0D) {
                    throw failure(itemLabel, "group width/height must be non-negative");
                }
                groups.add(new GuiDefinition.Group(id, parent,
                        children.containsKey(id) ? children.get(id) : Collections.<String>emptyList(),
                        optionalFiniteNumber(element, "x", 0.0D, itemLabel),
                        optionalFiniteNumber(element, "y", 0.0D, itemLabel), width, height, anchor,
                        parsePivot(element, width, height, itemLabel), order));
            }
        }
        return new ParsedScene(sprites, groups, new LinkedHashSet<String>(byId.keySet()), parents);
    }

    private static void visitScene(String id, Map<String, List<String>> children,
                                   Set<String> visiting, Set<String> visited,
                                   Map<String, Integer> order, int[] nextOrder,
                                   int depth, String label) {
        if (depth > ResourceLimits.MAX_SCENE_DEPTH) {
            throw failure(label, "scene depth exceeds " + ResourceLimits.MAX_SCENE_DEPTH + " at " + id);
        }
        if (visited.contains(id)) return;
        if (!visiting.add(id)) {
            StringBuilder cycle = new StringBuilder();
            for (String item : visiting) {
                if (cycle.length() > 0) cycle.append(" -> ");
                cycle.append(item);
            }
            cycle.append(" -> ").append(id);
            throw failure(label, "scene graph cycle detected: " + cycle);
        }
        order.put(id, Integer.valueOf(nextOrder[0]++));
        List<String> nested = children.get(id);
        if (nested != null) {
            for (String child : nested) visitScene(child, children, visiting, visited,
                    order, nextOrder, depth + 1, label);
        }
        visiting.remove(id);
        visited.add(id);
    }

    private Symbols parseSymbols(JsonObject definition, String label) {
        JsonObject configuredConstants = optionalObject(definition, "constants", label);
        if (configuredConstants.size() > ReactiveLimits.MAX_CONSTANTS_PER_DEFINITION) {
            throw failure(label, "constants exceeds " + ReactiveLimits.MAX_CONSTANTS_PER_DEFINITION);
        }
        Map<String, RuntimeValue> constants = new LinkedHashMap<String, RuntimeValue>();
        Map<String, ValueType> types = new LinkedHashMap<String, ValueType>(RuntimeVariables.stateTypes());
        for (Map.Entry<String, JsonElement> entry : configuredConstants.entrySet()) {
            String name = symbolName(entry.getKey(), label + ".constants");
            if (types.containsKey(name) || elementVariableTypes().containsKey(name)) {
                throw failure(label, "constant conflicts with reserved variable: " + name);
            }
            RuntimeValue value = primitiveValue(entry.getValue(), label + ".constants." + name);
            constants.put(name, value);
            types.put(name, value.getType());
        }

        JsonObject configuredValues = optionalObject(definition, "values", label);
        if (configuredValues.size() > ReactiveLimits.MAX_DERIVED_VALUES_PER_DEFINITION) {
            throw failure(label, "values exceeds " + ReactiveLimits.MAX_DERIVED_VALUES_PER_DEFINITION);
        }
        List<DerivedValue> values = new ArrayList<DerivedValue>();
        Set<String> unsupported = unsupportedVariables(RuntimeVariables.stateTypes().keySet());
        for (Map.Entry<String, JsonElement> entry : configuredValues.entrySet()) {
            String name = symbolName(entry.getKey(), label + ".values");
            if (types.containsKey(name) || elementVariableTypes().containsKey(name)) {
                throw failure(label, "derived value conflicts with existing variable: " + name);
            }
            if (!entry.getValue().isJsonPrimitive()
                    || !entry.getValue().getAsJsonPrimitive().isString()) {
                throw failure(label, "derived value " + name + " must be an expression string");
            }
            ExpressionCompiler compiler = new ExpressionCompiler(types, unsupported);
            CompiledExpression expression = compile(compiler, entry.getValue().getAsString(),
                    label + ".values." + name);
            values.add(new DerivedValue(name, expression));
            types.put(name, expression.getType());
        }
        return new Symbols(constants, values, types);
    }

    private static RuntimeValue primitiveValue(JsonElement value, String label) {
        if (!value.isJsonPrimitive()) throw failure(label, "constant must be number, boolean, or string");
        if (value.getAsJsonPrimitive().isBoolean()) return RuntimeValue.bool(value.getAsBoolean());
        if (value.getAsJsonPrimitive().isString()) return RuntimeValue.string(value.getAsString());
        if (value.getAsJsonPrimitive().isNumber()) {
            double number = value.getAsDouble();
            if (!Double.isFinite(number)) throw failure(label, "constant number must be finite");
            return RuntimeValue.number(number);
        }
        throw failure(label, "constant must be number, boolean, or string");
    }

    private static Map<String, ValueType> elementVariableTypes() {
        Map<String, ValueType> result = new LinkedHashMap<String, ValueType>();
        for (String prefix : Arrays.asList("self", "parent")) {
            result.put(prefix + ".local_x", ValueType.NUMBER);
            result.put(prefix + ".local_y", ValueType.NUMBER);
            result.put(prefix + ".world_x", ValueType.NUMBER);
            result.put(prefix + ".world_y", ValueType.NUMBER);
            result.put(prefix + ".world_center_x", ValueType.NUMBER);
            result.put(prefix + ".world_center_y", ValueType.NUMBER);
            result.put(prefix + ".width", ValueType.NUMBER);
            result.put(prefix + ".height", ValueType.NUMBER);
        }
        return result;
    }

    private static GuiDefinition.Anchor sceneAnchor(JsonObject element, String parent, String label) {
        String configured = string(element, "anchor", parent == null ? "gui" : "parent", label);
        GuiDefinition.Anchor anchor = parseSceneAnchor(configured, label);
        if (parent == null && anchor == GuiDefinition.Anchor.PARENT) {
            throw failure(label, "root element cannot use parent anchor");
        }
        if (parent != null && anchor != GuiDefinition.Anchor.PARENT) {
            throw failure(label, "child element anchor must be parent");
        }
        return anchor;
    }

    private static GuiDefinition.Anchor parseSceneAnchor(String value, String label) {
        try {
            return GuiDefinition.Anchor.valueOf(value.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException error) {
            throw failure(label, "unknown scene anchor: " + value);
        }
    }

    private static String compatibleBaseAnchor(String value, String label) {
        GuiDefinition.Anchor anchor = parseSceneAnchor(value, label);
        if (anchor == GuiDefinition.Anchor.GUI || anchor.name().startsWith("GUI_")) return "gui";
        if (anchor == GuiDefinition.Anchor.SCREEN_CENTER) return "screen_center";
        return "screen";
    }

    private static GuiDefinition.Pivot parsePivot(JsonObject element, double width, double height,
                                                  String label) {
        if (!element.has("pivot")) return new GuiDefinition.Pivot(width * 0.5D, height * 0.5D);
        JsonElement configured = element.get("pivot");
        if (configured.isJsonObject()) {
            JsonObject pivot = configured.getAsJsonObject();
            rejectUnknown(pivot, fields("x", "y"), label + ".pivot");
            return new GuiDefinition.Pivot(requiredFiniteNumber(pivot, "x", label + ".pivot"),
                    requiredFiniteNumber(pivot, "y", label + ".pivot"));
        }
        if (!configured.isJsonPrimitive() || !configured.getAsJsonPrimitive().isString()) {
            throw failure(label, "pivot must be a named origin or {x,y}");
        }
        String value = configured.getAsString();
        if ("parent".equals(value) || "top_left".equals(value)) return new GuiDefinition.Pivot(0, 0);
        double x;
        if (value.endsWith("_left")) x = 0.0D;
        else if (value.endsWith("_right")) x = width;
        else x = width * 0.5D;
        double y;
        if (value.startsWith("top_")) y = 0.0D;
        else if (value.startsWith("bottom_")) y = height;
        else if (value.startsWith("center_") || "center".equals(value)) y = height * 0.5D;
        else throw failure(label, "unknown pivot: " + value);
        return new GuiDefinition.Pivot(x, y);
    }

    private static double optionalFiniteNumber(JsonObject json, String field, double fallback, String label) {
        if (!json.has(field)) return fallback;
        double value = requiredFiniteNumber(json, field, label);
        return ResourceLimits.finiteGui(value, label + "." + field);
    }

    private static JsonObject optionalObject(JsonObject json, String field, String label) {
        if (!json.has(field)) return new JsonObject();
        return object(json.get(field), label + "." + field);
    }

    private static String symbolName(String value, String label) {
        if (value == null || !value.matches("[a-z_][a-z0-9_]*(\\.[a-z_][a-z0-9_]*)*")
                || value.length() > ResourceLimits.MAX_PATH_LENGTH) {
            throw failure(label, "invalid symbol name: " + value);
        }
        for (String reserved : Arrays.asList("player.", "screen.", "gui.", "mouse.",
                "event.", "self.", "parent.")) {
            if (value.startsWith(reserved)) {
                throw failure(label, "symbol uses reserved namespace: " + value);
            }
        }
        return value;
    }

    private ParsedElements attachElementIds(JsonObject definition, List<GuiDefinition.Sprite> parsed,
                                             String label) {
        JsonArray sprites = optionalArray(definition, "sprites", label);
        List<GuiDefinition.Sprite> result = new ArrayList<GuiDefinition.Sprite>(parsed.size());
        Set<String> ids = new LinkedHashSet<String>();
        for (int index = 0; index < sprites.size(); index++) {
            String itemLabel = label + ".sprites[" + index + "]";
            String id = stableId(requiredString(sprites.get(index).getAsJsonObject(), "id", itemLabel), itemLabel + ".id");
            if (!ids.add(id)) throw failure(itemLabel, "duplicate element id: " + id);
            GuiDefinition.Sprite sprite = parsed.get(index);
            result.add(new GuiDefinition.Sprite(sprite.texture(), sprite.animation(), sprite.x(), sprite.y(),
                    sprite.z(), sprite.u(), sprite.v(), sprite.width(), sprite.height(), sprite.sourceWidth(),
                    sprite.sourceHeight(), sprite.textureWidth(), sprite.textureHeight(), sprite.fullTexture(),
                    sprite.color(), sprite.layer(), sprite.anchor(), id));
        }
        return new ParsedElements(result, ids);
    }

    private ReactiveDefinition parseReactive(JsonObject definition, Set<String> elementIds, String label) {
        List<Binding> bindings = parseBindings(definition, elementIds, label, stateExpressions);
        Map<String, PropertyAnimation> animations = parseAnimations(definition, label, false);
        List<BehaviorRule> behaviors = parseBehaviors(definition, elementIds, animations, label,
                healthEventExpressions, stateEventExpressions);
        return new ReactiveDefinition(bindings, animations, behaviors);
    }

    private List<Binding> parseBindings(JsonObject definition, Set<String> elementIds, String label,
                                        ExpressionCompiler bindingCompiler) {
        return parseBindings(definition, elementIds, label, bindingCompiler, null);
    }

    private List<Binding> parseBindings(JsonObject definition, Set<String> elementIds, String label,
                                        ExpressionCompiler bindingCompiler,
                                        Map<String, String> parents) {
        JsonArray values = optionalArray(definition, "bindings", label);
        checkCount(values, ReactiveLimits.MAX_BINDINGS_PER_DEFINITION, label + ".bindings");
        List<Binding> result = new ArrayList<Binding>();
        Set<String> unique = new HashSet<String>();
        for (int index = 0; index < values.size(); index++) {
            String itemLabel = label + ".bindings[" + index + "]";
            JsonObject value = object(values.get(index), itemLabel);
            rejectUnknown(value, fields("target", "property", "value", "smoothing_ms"), itemLabel);
            String target = target(value, elementIds, itemLabel);
            ReactiveProperty property = property(requiredString(value, "property", itemLabel), itemLabel);
            requirePropertyCapability(property, itemLabel);
            String key = target + "\u0000" + property.getSchemaName();
            if (!unique.add(key)) throw failure(itemLabel, "duplicate binding for " + target + "." + property.getSchemaName());
            String expressionSource = requiredString(value, "value", itemLabel);
            if (parents != null && !parents.containsKey(target)
                    && referencesNamespace(expressionSource, "parent.")) {
                throw failure(itemLabel, "root target cannot reference parent.*");
            }
            CompiledExpression expression = compile(bindingCompiler, expressionSource, itemLabel);
            if (expression.getType() != property.getValueType()) {
                throw failure(itemLabel, "binding expression type must be " + property.getValueType());
            }
            int smoothing = value.has("smoothing_ms")
                    ? requiredNonNegativeInteger(value, "smoothing_ms", itemLabel) : 0;
            if (smoothing > ReactiveLimits.MAX_BINDING_SMOOTHING_MS) {
                throw failure(itemLabel, "smoothing_ms exceeds " + ReactiveLimits.MAX_BINDING_SMOOTHING_MS);
            }
            if (smoothing > 0 && property.getValueType() != ValueType.NUMBER) {
                throw failure(itemLabel, "smoothing_ms is only valid for numeric properties");
            }
            result.add(new Binding(target, property, expression, smoothing));
        }
        return result;
    }

    private Map<String, PropertyAnimation> parseAnimations(JsonObject definition, String label,
                                                            boolean allowComposition) {
        JsonArray values = optionalArray(definition, "animations", label);
        checkCount(values, ReactiveLimits.MAX_ANIMATIONS_PER_DEFINITION, label + ".animations");
        Map<String, PropertyAnimation> result = new LinkedHashMap<String, PropertyAnimation>();
        for (int index = 0; index < values.size(); index++) {
            String itemLabel = label + ".animations[" + index + "]";
            JsonObject value = object(values.get(index), itemLabel);
            rejectUnknown(value, fields("id", "duration_ms", "loop", "tracks"), itemLabel);
            String id = stableId(requiredString(value, "id", itemLabel), itemLabel + ".id");
            if (result.containsKey(id)) throw failure(itemLabel, "duplicate animation id: " + id);
            int duration = requiredPositiveDuration(value, "duration_ms", itemLabel);
            JsonArray tracks = requiredArray(value, "tracks", itemLabel);
            if (tracks.size() == 0 || tracks.size() > ReactiveLimits.MAX_TRACKS_PER_ANIMATION) {
                throw failure(itemLabel, "tracks must contain 1.." + ReactiveLimits.MAX_TRACKS_PER_ANIMATION + " entries");
            }
            List<PropertyTrack> parsedTracks = new ArrayList<PropertyTrack>();
            Set<ReactiveProperty> properties = EnumSet.noneOf(ReactiveProperty.class);
            for (int trackIndex = 0; trackIndex < tracks.size(); trackIndex++) {
                String trackLabel = itemLabel + ".tracks[" + trackIndex + "]";
                JsonObject track = object(tracks.get(trackIndex), trackLabel);
                rejectUnknown(track, allowComposition
                        ? fields("property", "interpolation", "compose", "keyframes")
                        : fields("property", "interpolation", "keyframes"), trackLabel);
                ReactiveProperty property = property(requiredString(track, "property", trackLabel), trackLabel);
                if (property == ReactiveProperty.VISIBLE) {
                    throw failure(trackLabel, "visible is not an animation track property");
                }
                requirePropertyCapability(property, trackLabel);
                if (!properties.add(property)) throw failure(trackLabel, "duplicate animation track property");
                String interpolationName = string(track, "interpolation", "linear", trackLabel);
                Interpolation interpolation;
                if ("linear".equals(interpolationName)) interpolation = Interpolation.LINEAR;
                else if ("smoothstep".equals(interpolationName)) interpolation = Interpolation.SMOOTHSTEP;
                else throw failure(trackLabel, "unknown interpolation: " + interpolationName);
                CompositionMode composition = CompositionMode.defaultFor(property);
                if (allowComposition && track.has("compose")) {
                    try {
                        composition = CompositionMode.parse(requiredString(track, "compose", trackLabel));
                    } catch (IllegalArgumentException error) {
                        throw failure(trackLabel, error.getMessage());
                    }
                }
                parsedTracks.add(new PropertyTrack(property, interpolation, composition,
                        parseKeyframes(requiredArray(track, "keyframes", trackLabel), duration, property, trackLabel)));
            }
            result.put(id, new PropertyAnimation(id, duration, bool(value, "loop", false, itemLabel), parsedTracks));
        }
        return result;
    }

    private List<PropertyKeyframe> parseKeyframes(JsonArray values, int duration,
                                                  ReactiveProperty property, String label) {
        if (values.size() == 0 || values.size() > ReactiveLimits.MAX_KEYFRAMES_PER_TRACK) {
            throw failure(label, "keyframes must contain 1.." + ReactiveLimits.MAX_KEYFRAMES_PER_TRACK + " entries");
        }
        List<PropertyKeyframe> result = new ArrayList<PropertyKeyframe>();
        long previous = -1L;
        for (int index = 0; index < values.size(); index++) {
            String itemLabel = label + ".keyframes[" + index + "]";
            JsonObject value = object(values.get(index), itemLabel);
            rejectUnknown(value, fields("time_ms", "value"), itemLabel);
            int time = requiredNonNegativeInteger(value, "time_ms", itemLabel);
            if (time > duration) throw failure(itemLabel, "time_ms exceeds animation duration");
            if (index == 0 && time != 0) throw failure(itemLabel, "first keyframe time_ms must be 0");
            if (time <= previous) throw failure(itemLabel, "keyframe times must be strictly increasing");
            double number = requiredFiniteNumber(value, "value", itemLabel);
            if ((property == ReactiveProperty.SCALE_X || property == ReactiveProperty.SCALE_Y)
                    && (number < 0.0D || number > 8.0D)) {
                throw failure(itemLabel, "scale keyframe value must be within 0..8");
            }
            if (property == ReactiveProperty.ROTATION_Z && (number < -360.0D || number > 360.0D)) {
                throw failure(itemLabel, "rotation_z keyframe value must be within -360..360");
            }
            result.add(new PropertyKeyframe(time, number));
            previous = time;
        }
        return result;
    }

    private List<BehaviorRule> parseBehaviors(JsonObject definition, Set<String> elementIds,
                                              Map<String, PropertyAnimation> animations, String label,
                                              ExpressionCompiler healthCompiler,
                                              ExpressionCompiler stateCompiler) {
        JsonArray values = optionalArray(definition, "behaviors", label);
        checkCount(values, ReactiveLimits.MAX_BEHAVIORS_PER_DEFINITION, label + ".behaviors");
        List<BehaviorRule> result = new ArrayList<BehaviorRule>();
        for (int index = 0; index < values.size(); index++) {
            String itemLabel = label + ".behaviors[" + index + "]";
            JsonObject value = object(values.get(index), itemLabel);
            rejectUnknown(value, fields("on", "if", "actions"), itemLabel);
            JsonObject on = requiredObject(value, "on", itemLabel);
            rejectUnknown(on, fields("event", "every", "mode"), itemLabel + ".on");
            String event = requiredString(on, "event", itemLabel + ".on");
            if (!EVENTS.contains(event)) throw failure(itemLabel, "unknown event: " + event);
            requireEventCapability(event, itemLabel);
            Double every = on.has("every") ? Double.valueOf(requiredFiniteNumber(on, "every", itemLabel + ".on")) : null;
            if (every != null && !isHealthEvent(event)) {
                throw failure(itemLabel, "every is only valid for health change events");
            }
            String mode = string(on, "mode", "coalesce", itemLabel + ".on");
            if (!"coalesce".equals(mode)) throw failure(itemLabel, "unknown event mode: " + mode);
            EventTrigger trigger = new EventTrigger(event, every, EventTrigger.Mode.COALESCE);
            ExpressionCompiler compiler = isHealthEvent(event) ? healthCompiler : stateCompiler;
            CompiledExpression condition = compile(compiler, string(value, "if", "true", itemLabel), itemLabel + ".if");
            if (condition.getType() != ValueType.BOOLEAN) throw failure(itemLabel, "behavior if must be boolean");
            JsonArray actions = requiredArray(value, "actions", itemLabel);
            if (actions.size() == 0 || actions.size() > ReactiveLimits.MAX_ACTIONS_PER_BEHAVIOR) {
                throw failure(itemLabel, "actions must contain 1.." + ReactiveLimits.MAX_ACTIONS_PER_BEHAVIOR + " entries");
            }
            List<Action> parsedActions = new ArrayList<Action>();
            for (int actionIndex = 0; actionIndex < actions.size(); actionIndex++) {
                parsedActions.add(parseAction(object(actions.get(actionIndex),
                        itemLabel + ".actions[" + actionIndex + "]"), elementIds, animations,
                        itemLabel + ".actions[" + actionIndex + "]"));
            }
            result.add(new BehaviorRule(trigger, condition, parsedActions));
        }
        return result;
    }

    private Action parseAction(JsonObject value, Set<String> elementIds,
                               Map<String, PropertyAnimation> animations, String label) {
        String type = requiredString(value, "type", label);
        if ("play_animation".equals(type)) {
            rejectUnknown(value, fields("type", "target", "animation", "restart"), label);
            requireCapability(Capability.ACTION_ANIMATION, label);
            String target = target(value, elementIds, label);
            String animation = requiredString(value, "animation", label);
            if (!animations.containsKey(animation)) throw failure(label, "unknown animation: " + animation);
            return new PlayAnimationAction(target, animation, bool(value, "restart", true, label));
        }
        if ("stop_animation".equals(type)) {
            rejectUnknown(value, fields("type", "target", "animation"), label);
            requireCapability(Capability.ACTION_ANIMATION, label);
            String target = target(value, elementIds, label);
            String animation = requiredString(value, "animation", label);
            if (!animations.containsKey(animation)) throw failure(label, "unknown animation: " + animation);
            return new StopAnimationAction(target, animation);
        }
        if ("set_visible".equals(type)) {
            rejectUnknown(value, fields("type", "target", "value"), label);
            requireCapability(Capability.ACTION_SET_VISIBLE, label);
            return new SetVisibleAction(target(value, elementIds, label), requiredBoolean(value, "value", label));
        }
        if ("set_alpha".equals(type)) {
            rejectUnknown(value, fields("type", "target", "value"), label);
            requireCapability(Capability.ACTION_SET_ALPHA, label);
            double alpha = requiredFiniteNumber(value, "value", label);
            if (alpha < 0.0D || alpha > 1.0D) throw failure(label, "set_alpha value must be within 0..1");
            return new SetAlphaAction(target(value, elementIds, label), alpha);
        }
        throw failure(label, "unknown action: " + type);
    }

    private void requirePropertyCapability(ReactiveProperty property, String label) {
        if (property == ReactiveProperty.VISIBLE) requireCapability(Capability.PROPERTY_VISIBLE, label);
        else if (property == ReactiveProperty.ALPHA) requireCapability(Capability.PROPERTY_ALPHA, label);
        else if (property == ReactiveProperty.SCALE_X || property == ReactiveProperty.SCALE_Y) {
            requireCapability(Capability.PROPERTY_SCALE, label);
        } else if (property == ReactiveProperty.ROTATION_Z) {
            requireCapability(Capability.PROPERTY_ROTATION, label);
        } else requireCapability(Capability.PROPERTY_TRANSLATE, label);
    }

    private void requireEventCapability(String event, String label) {
        if (isHealthEvent(event)) requireCapability(Capability.EVENT_HEALTH, label);
        else if (RuntimeEvent.SCREEN_OPENED.equals(event)) {
            requireCapability(Capability.EVENT_SCREEN_LIFECYCLE, label);
        } else requireCapability(Capability.EVENT_BURNING, label);
    }

    private void requireCapability(Capability capability, String label) {
        if (!capabilities.contains(capability)) {
            throw failure(label, "unsupported capability: " + capability.name());
        }
    }

    private Set<String> unsupportedVariables(Set<String> variables) {
        Set<String> result = new HashSet<String>();
        for (String variable : variables) {
            Capability capability = variableCapability(variable);
            if (capability != null && !capabilities.contains(capability)) result.add(variable);
        }
        return result;
    }

    private static Capability variableCapability(String variable) {
        if ("player.health".equals(variable)) return Capability.PLAYER_HEALTH;
        if ("player.max_health".equals(variable)) return Capability.PLAYER_MAX_HEALTH;
        if ("player.is_burning".equals(variable)) return Capability.PLAYER_BURNING;
        if ("player.is_sneaking".equals(variable)) return Capability.PLAYER_SNEAKING;
        if ("player.is_sprinting".equals(variable)) return Capability.PLAYER_SPRINTING;
        if ("player.armor".equals(variable)) return Capability.PLAYER_ARMOR;
        if ("player.food".equals(variable)) return Capability.PLAYER_FOOD;
        if ("player.air".equals(variable)) return Capability.PLAYER_AIR;
        if ("player.level".equals(variable)) return Capability.PLAYER_LEVEL;
        if ("player.experience".equals(variable)) return Capability.PLAYER_EXPERIENCE;
        if (variable.startsWith("screen.")) return Capability.SCREEN_SIZE;
        if ("gui.x".equals(variable) || "gui.y".equals(variable)) return Capability.GUI_POSITION;
        if ("gui.width".equals(variable) || "gui.height".equals(variable)) return Capability.GUI_SIZE;
        if ("mouse.left_down".equals(variable) || "mouse.right_down".equals(variable)) {
            return Capability.MOUSE_BUTTONS;
        }
        if (variable.startsWith("mouse.")) return Capability.MOUSE_POSITION;
        return null;
    }

    private static boolean isHealthEvent(String event) {
        return RuntimeEvent.HEALTH_DECREASED.equals(event) || RuntimeEvent.HEALTH_INCREASED.equals(event);
    }

    private static ReactiveProperty property(String value, String label) {
        try {
            return ReactiveProperty.parse(value);
        } catch (IllegalArgumentException error) {
            throw failure(label, error.getMessage());
        }
    }

    private static String target(JsonObject value, Set<String> elementIds, String label) {
        String target = requiredString(value, "target", label);
        if (!elementIds.contains(target)) throw failure(label, "unknown target element: " + target);
        return target;
    }

    private static CompiledExpression compile(ExpressionCompiler compiler, String source, String label) {
        try {
            return compiler.compile(source);
        } catch (IllegalArgumentException error) {
            throw failure(label, error.getMessage());
        }
    }

    /** Token-level namespace check used before the expression AST is internalized; string literals are ignored. */
    private static boolean referencesNamespace(String source, String namespace) {
        boolean quoted = false;
        boolean escaped = false;
        for (int index = 0; index < source.length();) {
            char current = source.charAt(index);
            if (quoted) {
                index++;
                if (escaped) escaped = false;
                else if (current == '\\') escaped = true;
                else if (current == '"') quoted = false;
                continue;
            }
            if (current == '"') {
                quoted = true;
                index++;
                continue;
            }
            if (Character.isLetter(current) || current == '_') {
                int end = index + 1;
                while (end < source.length()) {
                    char next = source.charAt(end);
                    if (!Character.isLetterOrDigit(next) && next != '_' && next != '.') break;
                    end++;
                }
                if (source.substring(index, end).startsWith(namespace)) return true;
                index = end;
                continue;
            }
            index++;
        }
        return false;
    }

    private static String stableId(String value, String label) {
        if (value.length() > ResourceLimits.MAX_PATH_LENGTH || !value.matches("[a-z0-9_.-]+")) {
            throw failure(label, "must be a lowercase stable element/animation id");
        }
        return value;
    }

    private static Set<String> fields(String... values) {
        return Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(values)));
    }

    private static void rejectUnknown(JsonObject json, Set<String> allowed, String label) {
        Set<String> unknown = new HashSet<String>(json.keySet());
        unknown.removeAll(allowed);
        if (!unknown.isEmpty()) throw failure(label, "unknown field(s): " + unknown);
    }

    private static JsonObject requiredObject(JsonObject json, String field, String label) {
        JsonElement value = json.get(field);
        if (value == null) throw failure(label, "missing required object: " + field);
        return object(value, label + "." + field);
    }

    private static JsonObject object(JsonElement value, String label) {
        if (value == null || !value.isJsonObject()) throw failure(label, "must be an object");
        return value.getAsJsonObject();
    }

    private static JsonArray requiredArray(JsonObject json, String field, String label) {
        JsonElement value = json.get(field);
        if (value == null || !value.isJsonArray()) throw failure(label, "missing or non-array field: " + field);
        return value.getAsJsonArray();
    }

    private static JsonArray optionalArray(JsonObject json, String field, String label) {
        JsonElement value = json.get(field);
        if (value == null || value.isJsonNull()) return new JsonArray();
        if (!value.isJsonArray()) throw failure(label, field + " must be an array");
        return value.getAsJsonArray();
    }

    private static String requiredString(JsonObject json, String field, String label) {
        JsonElement value = json.get(field);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw failure(label, "missing or non-string field: " + field);
        }
        String result = value.getAsString();
        if (result.length() == 0) throw failure(label, field + " must not be empty");
        return result;
    }

    private static String string(JsonObject json, String field, String fallback, String label) {
        return json.has(field) ? requiredString(json, field, label) : fallback;
    }

    private static boolean bool(JsonObject json, String field, boolean fallback, String label) {
        return json.has(field) ? requiredBoolean(json, field, label) : fallback;
    }

    private static boolean requiredBoolean(JsonObject json, String field, String label) {
        JsonElement value = json.get(field);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
            throw failure(label, "missing or non-boolean field: " + field);
        }
        return value.getAsBoolean();
    }

    private static int requiredInteger(JsonObject json, String field, String label) {
        double value = requiredFiniteNumber(json, field, label);
        if (value != Math.rint(value) || Math.abs(value) > Integer.MAX_VALUE) {
            throw failure(label, field + " must be an integer");
        }
        return (int) value;
    }

    /** Internal code 31 represents the external exact JSON number 3.1. */
    private static int requiredApiVersion(JsonObject json, String label) {
        JsonElement value = json.get("api_version");
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw failure(label, "api_version must be numeric 3 or 3.1");
        }
        BigDecimal version;
        try {
            version = value.getAsBigDecimal().stripTrailingZeros();
        } catch (RuntimeException error) {
            throw failure(label, "api_version must be numeric 3 or 3.1");
        }
        if (version.compareTo(new BigDecimal("3")) == 0) return 3;
        if (version.compareTo(new BigDecimal("3.1")) == 0) return 31;
        throw failure(label, "api_version must be 3 or 3.1");
    }

    private static int requiredNonNegativeInteger(JsonObject json, String field, String label) {
        int value = requiredInteger(json, field, label);
        if (value < 0) throw failure(label, field + " must be non-negative");
        return value;
    }

    private static int requiredPositiveDuration(JsonObject json, String field, String label) {
        int value = requiredInteger(json, field, label);
        if (value <= 0 || value > 600000) throw failure(label, field + " must be within 1..600000");
        return value;
    }

    private static double requiredFiniteNumber(JsonObject json, String field, String label) {
        JsonElement value = json.get(field);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw failure(label, "missing or non-number field: " + field);
        }
        double result = value.getAsDouble();
        if (Double.isNaN(result) || Double.isInfinite(result)) throw failure(label, field + " must be finite");
        return result;
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

    private static String label(String source, int index) {
        return source + " definition " + index;
    }

    private static IllegalArgumentException failure(String label, String message) {
        return new IllegalArgumentException(label + ": " + message);
    }

    private static final class ParsedElements {
        final List<GuiDefinition.Sprite> sprites;
        final Set<String> ids;

        ParsedElements(List<GuiDefinition.Sprite> sprites, Set<String> ids) {
            this.sprites = Collections.unmodifiableList(new ArrayList<GuiDefinition.Sprite>(sprites));
            this.ids = Collections.unmodifiableSet(new LinkedHashSet<String>(ids));
        }
    }

    private static final class ParsedScene {
        final List<GuiDefinition.Sprite> sprites;
        final List<GuiDefinition.Group> groups;
        final Set<String> ids;
        final Map<String, String> parents;

        ParsedScene(List<GuiDefinition.Sprite> sprites, List<GuiDefinition.Group> groups,
                    Set<String> ids, Map<String, String> parents) {
            this.sprites = Collections.unmodifiableList(new ArrayList<GuiDefinition.Sprite>(sprites));
            this.groups = Collections.unmodifiableList(new ArrayList<GuiDefinition.Group>(groups));
            this.ids = Collections.unmodifiableSet(new LinkedHashSet<String>(ids));
            this.parents = Collections.unmodifiableMap(new LinkedHashMap<String, String>(parents));
        }
    }

    private static final class Symbols {
        final Map<String, RuntimeValue> constants;
        final List<DerivedValue> values;
        final Map<String, ValueType> types;

        Symbols(Map<String, RuntimeValue> constants, List<DerivedValue> values,
                Map<String, ValueType> types) {
            this.constants = Collections.unmodifiableMap(new LinkedHashMap<String, RuntimeValue>(constants));
            this.values = Collections.unmodifiableList(new ArrayList<DerivedValue>(values));
            this.types = Collections.unmodifiableMap(new LinkedHashMap<String, ValueType>(types));
        }
    }
}
