package redfoxexpand.client.gui;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.IWorldNameable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class GuiTitleResolver {

    private GuiTitleResolver() {
    }

    static Set<String> resolve(GuiScreen screen) {
        Set<String> titles = new LinkedHashSet<String>();
        addKnownVanillaTitle(screen.getClass().getSimpleName(), titles);
        for (Field field : allFields(screen.getClass())) {
            Class<?> type = field.getType();
            if (type != String.class
                    && !IChatComponent.class.isAssignableFrom(type)
                    && !IInventory.class.isAssignableFrom(type)
                    && !IWorldNameable.class.isAssignableFrom(type)) {
                continue;
            }
            try {
                field.setAccessible(true);
                addValue(field.get(screen), titles);
            } catch (Exception ignored) {
            }
        }
        return titles;
    }

    private static void addKnownVanillaTitle(String screenName, Set<String> result) {
        String key = null;
        if ("GuiCrafting".equals(screenName)) {
            key = "container.crafting";
        } else if ("GuiRepair".equals(screenName)) {
            key = "container.repair";
        } else if ("GuiMerchant".equals(screenName)) {
            key = "entity.Villager.name";
        }
        if (key != null) {
            add(key, result);
            add(I18n.format(key), result);
        }
    }

    private static void addValue(Object value, Set<String> result) {
        if (value instanceof String) {
            add((String) value, result);
        }
        if (value instanceof IInventory) {
            addComponent(((IInventory) value).getDisplayName(), result);
        } else if (value instanceof IWorldNameable) {
            addComponent(((IWorldNameable) value).getDisplayName(), result);
        } else if (value instanceof IChatComponent) {
            addComponent((IChatComponent) value, result);
        }
    }

    private static void addComponent(IChatComponent component, Set<String> result) {
        if (component == null) {
            return;
        }
        add(component.getUnformattedText(), result);
        if (component instanceof ChatComponentTranslation) {
            add(((ChatComponentTranslation) component).getKey(), result);
        }
    }

    private static void add(String value, Set<String> result) {
        if (value != null && !value.trim().isEmpty()) {
            result.add(value);
        }
    }

    private static List<Field> allFields(Class<?> type) {
        List<Field> fields = new ArrayList<Field>();
        for (Class<?> cursor = type; cursor != null; cursor = cursor.getSuperclass()) {
            Collections.addAll(fields, cursor.getDeclaredFields());
        }
        return fields;
    }
}
