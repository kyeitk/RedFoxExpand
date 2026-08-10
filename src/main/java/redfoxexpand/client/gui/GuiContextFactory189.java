package redfoxexpand.client.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;
import redfoxexpand.core.GuiContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/** Builds the platform-neutral matcher/runtime context from one live 1.8.9 container screen. */
public final class GuiContextFactory189 {
    private GuiContextFactory189() { }

    public static GuiContext create(GuiContainer screen, int guiLeft, int guiTop,
                                    int guiWidth, int guiHeight) {
        Set<String> titles = GuiTitleResolver.resolve(screen);
        String titleKey = null;
        String titleText = null;
        for (String value : titles) {
            String translated = I18n.format(value);
            if (!translated.equals(value)) {
                if (titleKey == null) titleKey = value;
                if (titleText == null) titleText = translated;
            } else if (titleText == null) {
                titleText = value;
            }
        }
        List<GuiContext.SlotContext> slots = new ArrayList<GuiContext.SlotContext>();
        for (int index = 0; index < screen.inventorySlots.inventorySlots.size(); index++) {
            Slot slot = (Slot) screen.inventorySlots.inventorySlots.get(index);
            int x = slot instanceof SlotBaseAccess
                    ? ((SlotBaseAccess) slot).redfoxexpand$getBaseX() : slot.xDisplayPosition;
            int y = slot instanceof SlotBaseAccess
                    ? ((SlotBaseAccess) slot).redfoxexpand$getBaseY() : slot.yDisplayPosition;
            slots.add(new GuiContext.SlotContext(index, x, y, slot.getClass().getName(),
                    slot.getClass().getSimpleName(), hierarchy(slot.getClass(), false),
                    hierarchy(slot.getClass(), true)));
        }
        return new GuiContext(screen.getClass().getName(), hierarchy(screen.getClass(), false),
                screen.inventorySlots.getClass().getName(),
                hierarchy(screen.inventorySlots.getClass(), false),
                null, titleKey, titleText, screen.width, screen.height,
                guiLeft, guiTop, guiWidth, guiHeight, slots, null, null);
    }

    private static List<String> hierarchy(Class<?> type, boolean simple) {
        List<String> result = new ArrayList<String>();
        collect(type, simple, result);
        return Collections.unmodifiableList(result);
    }

    private static void collect(Class<?> type, boolean simple, List<String> result) {
        if (type == null) return;
        String value = simple ? type.getSimpleName() : type.getName();
        if (!result.contains(value)) result.add(value);
        for (Class<?> contract : type.getInterfaces()) collect(contract, simple, result);
        collect(type.getSuperclass(), simple, result);
    }
}
