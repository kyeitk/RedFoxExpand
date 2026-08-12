package redfoxexpand.platform.fabric262;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import redfoxexpand.core.GuiContext;
import redfoxexpand.mixin.client.AbstractContainerScreenAccessor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Minecraft 26.2-only extraction of the platform-neutral GuiContext. */
public final class Fabric262GuiContextFactory {
    private static final Map<Class<?>, Hierarchy> HIERARCHIES = new ConcurrentHashMap<>();

    private Fabric262GuiContextFactory() {
    }

    public static GuiContext create(AbstractContainerScreen<?> screen, Identifier currentResource) {
        AbstractContainerScreenAccessor access = (AbstractContainerScreenAccessor) screen;
        AbstractContainerMenu menu = screen.getMenu();
        Hierarchy screenHierarchy = hierarchy(screen.getClass());
        Hierarchy menuHierarchy = hierarchy(menu.getClass());
        Component title = screen.getTitle();
        String titleKey = title.getContents() instanceof TranslatableContents translatable
                ? translatable.getKey() : null;
        String menuType = menuType(menu);
        String resource = currentResource == null ? null : currentResource.toString();
        String namespace = currentResource != null ? currentResource.getNamespace()
                : menuType != null ? Identifier.parse(menuType).getNamespace()
                : screen.getClass().getName().startsWith("net.minecraft.") ? "minecraft"
                : packageNamespace(screen.getClass());

        List<GuiContext.SlotContext> slots = new ArrayList<>(menu.slots.size());
        for (Slot slot : menu.slots) {
            Hierarchy slotHierarchy = hierarchy(slot.getClass());
            slots.add(new GuiContext.SlotContext(slot.index, slot.x, slot.y,
                    slot.getClass().getName(), slot.getClass().getSimpleName(),
                    slotHierarchy.full(), slotHierarchy.simple()));
        }
        return new GuiContext(screen.getClass().getName(), screenHierarchy.full(),
                menu.getClass().getName(), menuHierarchy.full(), menuType, titleKey,
                title.getString(), screen.width, screen.height,
                access.redfoxexpand$getLeftPos(), access.redfoxexpand$getTopPos(),
                access.redfoxexpand$getImageWidth(), access.redfoxexpand$getImageHeight(),
                slots, resource, namespace);
    }

    private static String menuType(AbstractContainerMenu menu) {
        try {
            return BuiltInRegistries.MENU.getKey(menu.getType()).toString();
        } catch (UnsupportedOperationException error) {
            return null;
        }
    }

    private static String packageNamespace(Class<?> type) {
        String packageName = type.getPackageName().toLowerCase(java.util.Locale.ROOT);
        int split = packageName.indexOf('.');
        String candidate = split < 0 ? packageName : packageName.substring(0, split);
        return candidate.matches("[a-z0-9_.-]+") && !candidate.isEmpty() ? candidate : "unknown";
    }

    private static Hierarchy hierarchy(Class<?> root) {
        return HIERARCHIES.computeIfAbsent(root, type -> {
            LinkedHashSet<Class<?>> visited = new LinkedHashSet<>();
            ArrayDeque<Class<?>> queue = new ArrayDeque<>();
            queue.add(type);
            while (!queue.isEmpty()) {
                Class<?> next = queue.removeFirst();
                if (!visited.add(next)) continue;
                Class<?> parent = next.getSuperclass();
                if (parent != null) queue.addLast(parent);
                for (Class<?> implemented : next.getInterfaces()) queue.addLast(implemented);
            }
            List<String> full = visited.stream().map(Class::getName).toList();
            List<String> simple = visited.stream().map(Class::getSimpleName).toList();
            return new Hierarchy(full, simple);
        });
    }

    private record Hierarchy(List<String> full, List<String> simple) { }
}

