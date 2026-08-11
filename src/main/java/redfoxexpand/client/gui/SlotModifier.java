package redfoxexpand.client.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class SlotModifier {

    private final Set<Integer> slots;
    private final Integer targetX;
    private final Integer targetY;
    private final String targetClass;
    private final ClassMatchMode targetClassMatch;
    public final int xOffset;
    public final int yOffset;
    public final Integer color;
    public final Integer color2;

    private SlotModifier(
            Set<Integer> slots,
            Integer targetX,
            Integer targetY,
            String targetClass,
            ClassMatchMode targetClassMatch,
            int xOffset,
            int yOffset,
            Integer color,
            Integer color2
    ) {
        this.slots = Collections.unmodifiableSet(slots);
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetClass = targetClass;
        this.targetClassMatch = targetClassMatch;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.color = color;
        this.color2 = color2;
    }

    public static SlotModifier parse(JsonObject json) {
        Set<Integer> slots = new LinkedHashSet<Integer>();
        JsonElement slotElement = json.get("slots");
        if (slotElement != null && !slotElement.isJsonNull()) {
            if (slotElement.isJsonArray()) {
                JsonArray array = slotElement.getAsJsonArray();
                for (JsonElement element : array) {
                    addSlotExpression(slots, element);
                }
            } else {
                addSlotExpression(slots, slotElement);
            }
        }

        Integer color = JsonSupport.color(json, "highlight_color");
        if (color == null) {
            color = JsonSupport.color(json, "color");
        }
        return new SlotModifier(
                slots,
                json.has("target_x") ? JsonSupport.integer(json, "target_x", 0) : null,
                json.has("target_y") ? JsonSupport.integer(json, "target_y", 0) : null,
                JsonSupport.string(json, "target_class_name",
                        JsonSupport.string(json, "target_class", null)),
                ClassMatchMode.parse(JsonSupport.string(json, "target_class_match", "exact")),
                JsonSupport.integer(json, "x_offset", 0),
                JsonSupport.integer(json, "y_offset", 0),
                color,
                JsonSupport.color(json, "color_2")
        );
    }

    public static SlotModifier fromNative(redfoxexpand.core.GuiDefinition.SlotRule rule) {
        return new SlotModifier(
                new LinkedHashSet<Integer>(rule.slots()),
                rule.targetX(),
                rule.targetY(),
                rule.targetClass(),
                rule.classMatchMode() == redfoxexpand.core.GuiDefinition.ClassMatchMode.ASSIGNABLE
                        ? ClassMatchMode.ASSIGNABLE : ClassMatchMode.EXACT,
                rule.xOffset(),
                rule.yOffset(),
                rule.highlightColor(),
                rule.highlightColor2()
        );
    }

    private static void addSlotExpression(Set<Integer> slots, JsonElement element) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            slots.add(element.getAsInt());
            return;
        }

        String expression = element.getAsString().trim();
        int separator = expression.indexOf('-');
        if (separator < 0) {
            slots.add(Integer.parseInt(expression));
            return;
        }

        int start = Integer.parseInt(expression.substring(0, separator).trim());
        int end = Integer.parseInt(expression.substring(separator + 1).trim());
        int direction = start <= end ? 1 : -1;
        int count = 0;
        for (int value = start; ; value += direction) {
            slots.add(value);
            if (value == end) {
                break;
            }
            if (++count > 4096) {
                throw new IllegalArgumentException("Slot range is too large: " + expression);
            }
        }
    }

    public boolean matches(Container container, Slot slot) {
        int index = container.inventorySlots.indexOf(slot);
        int baseX = slot instanceof SlotBaseAccess
                ? ((SlotBaseAccess) slot).redfoxexpand$getBaseX()
                : slot.xDisplayPosition;
        int baseY = slot instanceof SlotBaseAccess
                ? ((SlotBaseAccess) slot).redfoxexpand$getBaseY()
                : slot.yDisplayPosition;
        if (!slots.isEmpty() && !slots.contains(index)) {
            return false;
        }
        if (targetX != null && targetX.intValue() != baseX) {
            return false;
        }
        if (targetY != null && targetY.intValue() != baseY) {
            return false;
        }
        if (targetClass != null
                && !ClassNameMatcher.matches(targetClass, slot.getClass(), targetClassMatch)) {
            return false;
        }
        return true;
    }

    public void apply(Slot slot) {
        slot.xDisplayPosition += xOffset;
        slot.yDisplayPosition += yOffset;
    }

    public boolean hasHighlight() {
        return color != null || color2 != null;
    }

    public int firstHighlightColor() {
        return color == null ? 0x80FFFFFF : color.intValue();
    }

    public int secondHighlightColor() {
        return color2 == null ? firstHighlightColor() : color2.intValue();
    }
}
