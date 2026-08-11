package redfoxexpand.client.gui;

import net.minecraft.client.Minecraft;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Advances Schema v3 state exactly once from Forge's END client-tick bus. */
public final class ReactiveTickHandler {
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (Minecraft.getMinecraft().currentScreen instanceof GuiModifierScreenAccess) {
            ((GuiModifierScreenAccess) Minecraft.getMinecraft().currentScreen)
                    .redfoxexpand$tickReactive();
        }
    }
}
