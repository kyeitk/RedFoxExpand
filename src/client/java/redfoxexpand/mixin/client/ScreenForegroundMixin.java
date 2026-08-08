package redfoxexpand.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import redfoxexpand.client.gui.ScreenController;

@Mixin(Screen.class)
public abstract class ScreenForegroundMixin {
    @Inject(
            method = "extractRenderStateWithTooltipAndSubtitles",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;extractDeferredElements(IIF)V")
    )
    private void redfoxexpand$foregroundBeforeTooltips(GuiGraphicsExtractor graphics,
                                                       int mouseX, int mouseY, float tickProgress,
                                                       CallbackInfo callback) {
        ScreenController.INSTANCE.renderForeground((Screen) (Object) this, graphics, mouseX, mouseY);
    }
}
