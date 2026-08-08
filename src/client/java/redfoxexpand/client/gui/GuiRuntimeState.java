package redfoxexpand.client.gui;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import redfoxexpand.client.resource.ResourceSnapshot;
import redfoxexpand.core.GuiContext;
import redfoxexpand.core.ResolvedModifier;

import java.util.IdentityHashMap;
import java.util.Map;

/** Mutable per-screen ownership state; all resource/config data remains immutable. */
final class GuiRuntimeState {
    final AbstractContainerScreen<?> screen;
    final Map<Slot, Delta> slotDeltas = new IdentityHashMap<>();
    int leftDelta;
    int topDelta;
    Identifier currentGuiResource;
    ResourceSnapshot snapshot;
    GuiContext baseContext;
    ResolvedModifier modifier;

    GuiRuntimeState(AbstractContainerScreen<?> screen) {
        this.screen = screen;
    }

    record Delta(int x, int y) { }
}
