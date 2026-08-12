package redfoxexpand.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import redfoxexpand.client.gui.ScreenController;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenTextMixin {
    @Inject(method = "extractLabels", at = @At("HEAD"), cancellable = true)
    private void redfoxexpand$semanticInventoryTitle(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                                     CallbackInfo callback) {
        if (ScreenController.INSTANCE.renderSemanticLabels(
                (InventoryScreen) (Object) this, graphics, true)) {
            callback.cancel();
        }
    }
}

