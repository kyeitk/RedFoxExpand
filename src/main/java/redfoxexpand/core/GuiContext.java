package redfoxexpand.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable, platform-neutral facts used by matcher and transform evaluation. */
public final class GuiContext {
    private final String screenClass, menuClass, menuType, screenTitleKey, screenTitleText;
    private final String resourceLocation, modNamespace;
    private final List<String> screenHierarchy, menuHierarchy;
    private final int screenWidth, screenHeight, leftPos, topPos, imageWidth, imageHeight;
    private final List<SlotContext> slots;

    public GuiContext(String screenClass, List<String> screenHierarchy, String menuClass,
                      List<String> menuHierarchy, String menuType, String screenTitleKey,
                      String screenTitleText, int screenWidth, int screenHeight, int leftPos,
                      int topPos, int imageWidth, int imageHeight, List<SlotContext> slots,
                      String resourceLocation, String modNamespace) {
        this.screenClass = screenClass; this.menuClass = menuClass; this.menuType = menuType;
        this.screenTitleKey = screenTitleKey; this.screenTitleText = screenTitleText;
        this.screenWidth = screenWidth; this.screenHeight = screenHeight; this.leftPos = leftPos;
        this.topPos = topPos; this.imageWidth = imageWidth; this.imageHeight = imageHeight;
        this.resourceLocation = resourceLocation; this.modNamespace = modNamespace;
        this.screenHierarchy = immutable(screenHierarchy); this.menuHierarchy = immutable(menuHierarchy);
        this.slots = Collections.unmodifiableList(new ArrayList<SlotContext>(slots));
    }

    private static <T> List<T> immutable(List<T> values) {
        return Collections.unmodifiableList(new ArrayList<T>(values));
    }
    public String screenClass() { return screenClass; }
    public List<String> screenHierarchy() { return screenHierarchy; }
    public String menuClass() { return menuClass; }
    public List<String> menuHierarchy() { return menuHierarchy; }
    public String menuType() { return menuType; }
    public String screenTitleKey() { return screenTitleKey; }
    public String screenTitleText() { return screenTitleText; }
    public int screenWidth() { return screenWidth; }
    public int screenHeight() { return screenHeight; }
    public int leftPos() { return leftPos; }
    public int topPos() { return topPos; }
    public int imageWidth() { return imageWidth; }
    public int imageHeight() { return imageHeight; }
    public List<SlotContext> slots() { return slots; }
    public String resourceLocation() { return resourceLocation; }
    public String modNamespace() { return modNamespace; }

    public static final class SlotContext {
        private final int index, x, y;
        private final String className, simpleClassName;
        private final List<String> hierarchy, simpleHierarchy;
        public SlotContext(int index, int x, int y, String className, String simpleClassName,
                           List<String> hierarchy, List<String> simpleHierarchy) {
            this.index = index; this.x = x; this.y = y; this.className = className;
            this.simpleClassName = simpleClassName; this.hierarchy = immutable(hierarchy);
            this.simpleHierarchy = immutable(simpleHierarchy);
        }
        public int index() { return index; }
        public int x() { return x; }
        public int y() { return y; }
        public String className() { return className; }
        public String simpleClassName() { return simpleClassName; }
        public List<String> hierarchy() { return hierarchy; }
        public List<String> simpleHierarchy() { return simpleHierarchy; }
    }
}

