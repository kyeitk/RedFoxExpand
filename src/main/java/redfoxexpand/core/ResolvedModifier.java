package redfoxexpand.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Runtime merge of all active definitions matching one GUI context. */
public final class ResolvedModifier {
    private final GuiDefinition.Geometry geometry;
    private final List<GuiDefinition.SlotRule> slotRules;
    private final List<GuiDefinition.Sprite> sprites;
    private final List<GuiDefinition.TextOverlay> texts;
    private final List<GuiDefinition.TextRule> textRules;
    private final List<DefinitionCandidate> matchedDefinitions;

    public ResolvedModifier(GuiDefinition.Geometry geometry, List<GuiDefinition.SlotRule> slotRules,
                            List<GuiDefinition.Sprite> sprites, List<GuiDefinition.TextOverlay> texts,
                            List<GuiDefinition.TextRule> textRules) {
        this(geometry, slotRules, sprites, texts, textRules,
                Collections.<DefinitionCandidate>emptyList());
    }
    public ResolvedModifier(GuiDefinition.Geometry geometry, List<GuiDefinition.SlotRule> slotRules,
                            List<GuiDefinition.Sprite> sprites, List<GuiDefinition.TextOverlay> texts,
                            List<GuiDefinition.TextRule> textRules,
                            List<DefinitionCandidate> matchedDefinitions) {
        this.geometry = geometry; this.slotRules = immutable(slotRules); this.sprites = immutable(sprites);
        this.texts = immutable(texts); this.textRules = immutable(textRules);
        this.matchedDefinitions = immutable(matchedDefinitions);
    }
    private static <T> List<T> immutable(List<T> values) {
        return Collections.unmodifiableList(new ArrayList<T>(values));
    }
    public GuiDefinition.Geometry geometry() { return geometry; }
    public List<GuiDefinition.SlotRule> slotRules() { return slotRules; }
    public List<GuiDefinition.Sprite> sprites() { return sprites; }
    public List<GuiDefinition.TextOverlay> texts() { return texts; }
    public List<GuiDefinition.TextRule> textRules() { return textRules; }
    public List<DefinitionCandidate> matchedDefinitions() { return matchedDefinitions; }

    public static ResolvedModifier resolve(List<DefinitionCandidate> active, GuiContext context) {
        int x = 0, y = 0, width = 0, height = 0;
        List<GuiDefinition.SlotRule> slots = new ArrayList<GuiDefinition.SlotRule>();
        List<GuiDefinition.Sprite> sprites = new ArrayList<GuiDefinition.Sprite>();
        List<GuiDefinition.TextOverlay> texts = new ArrayList<GuiDefinition.TextOverlay>();
        List<GuiDefinition.TextRule> textRules = new ArrayList<GuiDefinition.TextRule>();
        List<DefinitionCandidate> matched = new ArrayList<DefinitionCandidate>();
        for (DefinitionCandidate candidate : active) {
            if (!candidate.matcher().matches(context)) continue;
            matched.add(candidate);
            GuiDefinition definition = candidate.definition();
            x = safeAdd(x, definition.geometry().xOffset(), "x_offset");
            y = safeAdd(y, definition.geometry().yOffset(), "y_offset");
            width = safeAdd(width, definition.geometry().widthOffset(), "width_offset");
            height = safeAdd(height, definition.geometry().heightOffset(), "height_offset");
            slots.addAll(definition.slotRules()); sprites.addAll(definition.sprites());
            texts.addAll(definition.texts()); textRules.addAll(definition.textRules());
        }
        Collections.sort(sprites, new Comparator<GuiDefinition.Sprite>() {
            @Override public int compare(GuiDefinition.Sprite left, GuiDefinition.Sprite right) {
                int layer = left.layer().compareTo(right.layer());
                return layer != 0 ? layer : Double.compare(left.z(), right.z());
            }
        });
        return new ResolvedModifier(new GuiDefinition.Geometry(x, y, width, height),
                slots, sprites, texts, textRules, matched);
    }
    public boolean isEmpty() {
        if (!geometry.equals(GuiDefinition.Geometry.ZERO) || !slotRules.isEmpty()
                || !sprites.isEmpty() || !texts.isEmpty() || !textRules.isEmpty()) return false;
        for (DefinitionCandidate candidate : matchedDefinitions) {
            if (!candidate.definition().reactive().isEmpty()) return false;
        }
        return true;
    }
    private static int safeAdd(int left, int right, String field) {
        return ResourceLimits.boundedInt((double) left + right, field);
    }
}
