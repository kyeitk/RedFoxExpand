package redfoxexpand.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Runtime merge of all active definitions matching one GUI context. */
public record ResolvedModifier(
        GuiDefinition.Geometry geometry,
        List<GuiDefinition.SlotRule> slotRules,
        List<GuiDefinition.Sprite> sprites,
        List<GuiDefinition.TextOverlay> texts,
        List<GuiDefinition.TextRule> textRules,
        List<DefinitionCandidate> matchedDefinitions
) {
    public ResolvedModifier {
        slotRules = List.copyOf(slotRules);
        sprites = List.copyOf(sprites);
        texts = List.copyOf(texts);
        textRules = List.copyOf(textRules);
        matchedDefinitions = List.copyOf(matchedDefinitions);
    }

    public ResolvedModifier(GuiDefinition.Geometry geometry, List<GuiDefinition.SlotRule> slotRules,
                            List<GuiDefinition.Sprite> sprites, List<GuiDefinition.TextOverlay> texts,
                            List<GuiDefinition.TextRule> textRules) {
        this(geometry, slotRules, sprites, texts, textRules, List.of());
    }

    public static ResolvedModifier resolve(List<DefinitionCandidate> active, GuiContext context) {
        int x = 0, y = 0, width = 0, height = 0;
        List<GuiDefinition.SlotRule> slots = new ArrayList<>();
        List<GuiDefinition.Sprite> sprites = new ArrayList<>();
        List<GuiDefinition.TextOverlay> texts = new ArrayList<>();
        List<GuiDefinition.TextRule> textRules = new ArrayList<>();
        List<DefinitionCandidate> matched = new ArrayList<>();
        for (DefinitionCandidate candidate : active) {
            if (!candidate.matcher().matches(context)) continue;
            matched.add(candidate);
            GuiDefinition definition = candidate.definition();
            x = safeAdd(x, definition.geometry().xOffset(), "x_offset");
            y = safeAdd(y, definition.geometry().yOffset(), "y_offset");
            width = safeAdd(width, definition.geometry().widthOffset(), "width_offset");
            height = safeAdd(height, definition.geometry().heightOffset(), "height_offset");
            slots.addAll(definition.slotRules());
            sprites.addAll(definition.sprites());
            texts.addAll(definition.texts());
            textRules.addAll(definition.textRules());
        }
        sprites.sort(Comparator.comparing(GuiDefinition.Sprite::layer)
                .thenComparingDouble(GuiDefinition.Sprite::z)
                .thenComparingInt(GuiDefinition.Sprite::sceneOrder));
        return new ResolvedModifier(new GuiDefinition.Geometry(x, y, width, height),
                slots, sprites, texts, textRules, matched);
    }

    public boolean isEmpty() {
        return geometry.equals(GuiDefinition.Geometry.ZERO) && slotRules.isEmpty()
                && sprites.isEmpty() && texts.isEmpty() && textRules.isEmpty()
                && matchedDefinitions.stream().allMatch(candidate -> candidate.definition().reactive().isEmpty());
    }

    private static int safeAdd(int left, int right, String field) {
        return ResourceLimits.boundedInt((double) left + right, field);
    }
}
