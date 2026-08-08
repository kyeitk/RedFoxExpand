package redfoxexpand.client.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

/** Extraction-thread capture of the last raw GUI texture used by the background phase. */
public final class GuiResourceCapture {
    private static final ThreadLocal<Capture> CURRENT = new ThreadLocal<>();

    private GuiResourceCapture() {
    }

    static void begin(Screen screen) {
        CURRENT.set(new Capture(screen, null));
    }

    public static void accept(Identifier resource) {
        Capture capture = CURRENT.get();
        if (capture != null) CURRENT.set(new Capture(capture.screen(), resource));
    }

    static Identifier finish(Screen screen) {
        Capture capture = CURRENT.get();
        CURRENT.remove();
        return capture != null && capture.screen() == screen ? capture.resource() : null;
    }

    static void clear() {
        CURRENT.remove();
    }

    private record Capture(Screen screen, Identifier resource) { }
}
