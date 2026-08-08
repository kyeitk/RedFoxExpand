package redfoxexpand.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import redfoxexpand.client.gui.ScreenController;

/** Places custom underlays after Screen's global dim/blur and before a container resumes drawing its background. */
@Mixin(Screen.class)
public abstract class ScreenUnderlayMixin {
    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void redfoxexpand$underlayAfterGlobalBackground(GuiGraphicsExtractor graphics,
                                                            int mouseX, int mouseY, float tickProgress,
                                                            CallbackInfo callback) {
        ScreenController.INSTANCE.renderUnderlayAfterScreenBackground(
                (Screen) (Object) this, graphics, mouseX, mouseY);
    }
}
