package redfoxexpand.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import redfoxexpand.client.gui.ScreenController;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenTextMixin {
    @Inject(method = "extractLabels", at = @At("HEAD"), cancellable = true)
    private void redfoxexpand$semanticLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                             CallbackInfo callback) {
        if (ScreenController.INSTANCE.renderSemanticLabels(
                (AbstractContainerScreen<?>) (Object) this, graphics, false)) {
            callback.cancel();
        }
    }
}

