package redfoxexpand.client.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public final class GuiEventHandler {

    private final GuiModifierManager manager;

    public GuiEventHandler(GuiModifierManager manager) {
        this.manager = manager;
    }

    @SubscribeEvent
    public void onPostInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.gui instanceof GuiContainer) {
            manager.onPostInit((GuiContainer) event.gui);
        }
    }
}
