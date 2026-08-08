package redfoxexpand.core;

import java.util.List;

/** Immutable, platform-neutral facts used by matcher and transform evaluation. */
public record GuiContext(
        String screenClass,
        List<String> screenHierarchy,
        String menuClass,
        List<String> menuHierarchy,
        String menuType,
        String screenTitleKey,
        String screenTitleText,
        int screenWidth,
        int screenHeight,
        int leftPos,
        int topPos,
        int imageWidth,
        int imageHeight,
        List<SlotContext> slots,
        String resourceLocation,
        String modNamespace
) {
    public GuiContext {
        screenHierarchy = List.copyOf(screenHierarchy);
        menuHierarchy = List.copyOf(menuHierarchy);
        slots = List.copyOf(slots);
    }

    public record SlotContext(
            int index,
            int x,
            int y,
            String className,
            String simpleClassName,
            List<String> hierarchy,
            List<String> simpleHierarchy
    ) {
        public SlotContext {
            hierarchy = List.copyOf(hierarchy);
            simpleHierarchy = List.copyOf(simpleHierarchy);
        }
    }
}
