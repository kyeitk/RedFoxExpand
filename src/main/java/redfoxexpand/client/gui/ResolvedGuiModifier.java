package redfoxexpand.client.gui;

import redfoxexpand.client.config.GuiDefinition;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Comparator;
import redfoxexpand.core.DefinitionCandidate;

public final class ResolvedGuiModifier {

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
    private final List<SlotModifier> slotModifiers;
    public final List<SpriteOverlay> sprites;
    public final List<TextOverlay> texts;
    private final List<FontRule> fontRules;
    public final List<DefinitionCandidate> reactiveDefinitions;

    private ResolvedGuiModifier(
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
            List<FontRule> fontRules,
            List<DefinitionCandidate> reactiveDefinitions
    ) {
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
        this.fontRules = Collections.unmodifiableList(fontRules);
        this.reactiveDefinitions = Collections.unmodifiableList(reactiveDefinitions);
    }

    static ResolvedGuiModifier merge(List<GuiDefinition> modifiers) {
        int xOffset = 0;
        int yOffset = 0;
        int widthOffset = 0;
        int heightOffset = 0;
        int titleXOffset = 0;
        int titleYOffset = 0;
        int labelXOffset = 0;
        int labelYOffset = 0;
        Integer titleColor = null;
        Integer labelColor = null;
        boolean titleHidden = false;
        boolean labelHidden = false;
        List<SlotModifier> slots = new ArrayList<SlotModifier>();
        List<SpriteOverlay> sprites = new ArrayList<SpriteOverlay>();
        List<SpriteOverlay> nativeSprites = new ArrayList<SpriteOverlay>();
        List<TextOverlay> texts = new ArrayList<TextOverlay>();
        List<FontRule> fontRules = new ArrayList<FontRule>();
        List<DefinitionCandidate> reactiveDefinitions = new ArrayList<DefinitionCandidate>();

        for (GuiDefinition modifier : modifiers) {
            xOffset += modifier.xOffset;
            yOffset += modifier.yOffset;
            widthOffset += modifier.widthOffset;
            heightOffset += modifier.heightOffset;
            titleXOffset += modifier.titleXOffset;
            titleYOffset += modifier.titleYOffset;
            labelXOffset += modifier.labelXOffset;
            labelYOffset += modifier.labelYOffset;
            if (modifier.titleColor != null) {
                titleColor = modifier.titleColor;
            }
            if (modifier.labelColor != null) {
                labelColor = modifier.labelColor;
            }
            titleHidden |= modifier.titleHidden;
            labelHidden |= modifier.labelHidden;
            slots.addAll(modifier.slotModifiers);
            if (modifier.nativeCandidate == null) sprites.addAll(modifier.sprites);
            else nativeSprites.addAll(modifier.sprites);
            texts.addAll(modifier.texts);
            fontRules.addAll(modifier.fontRules);
            if (modifier.nativeCandidate != null && (modifier.nativeCandidate.apiVersion() == 3
                    || modifier.nativeCandidate.apiVersion() == 31)) {
                reactiveDefinitions.add(modifier.nativeCandidate);
            }
        }
        Collections.sort(nativeSprites, new Comparator<SpriteOverlay>() {
            @Override
            public int compare(SpriteOverlay left, SpriteOverlay right) {
                int layer = left.layer.compareTo(right.layer);
                if (layer != 0) return layer;
                int z = Float.compare(left.z, right.z);
                if (z != 0) return z;
                int leftOrder = left.nativeSprite == null ? 0 : left.nativeSprite.sceneOrder();
                int rightOrder = right.nativeSprite == null ? 0 : right.nativeSprite.sceneOrder();
                return Integer.compare(leftOrder, rightOrder);
            }
        });
        sprites.addAll(nativeSprites);

        return new ResolvedGuiModifier(
                xOffset,
                yOffset,
                widthOffset,
                heightOffset,
                titleXOffset,
                titleYOffset,
                labelXOffset,
                labelYOffset,
                titleColor,
                labelColor,
                titleHidden,
                labelHidden,
                slots,
                sprites,
                texts,
                fontRules,
                reactiveDefinitions
        );
    }

    FontRule matchingFontRule(String text, int x, int y, int ordinal) {
        for (FontRule rule : fontRules) {
            if (rule.matches(text, x, y, ordinal)) return rule;
        }
        return null;
    }

    public List<SlotModifier> matchingSlots(Container container, Slot slot) {
        List<SlotModifier> matches = new ArrayList<SlotModifier>();
        for (SlotModifier modifier : slotModifiers) {
            if (modifier.matches(container, slot)) {
                matches.add(modifier);
            }
        }
        return matches;
    }

    public void renderForegroundText() {
        renderTextLayer(SpriteOverlay.Layer.FOREGROUND, 0, 0, 0, 0, 0, 0, true);
    }

    public void renderTextLayer(SpriteOverlay.Layer layer, int guiLeft, int guiTop,
                                int guiWidth, int guiHeight,
                                int screenWidth, int screenHeight, boolean matrixAtGuiOrigin) {
        for (TextOverlay text : texts) {
            if (text.layer == layer) {
                text.renderAnchored(guiLeft, guiTop, guiWidth, guiHeight,
                        screenWidth, screenHeight, matrixAtGuiOrigin);
            }
        }
    }
}
