package redfoxexpand.mixin;

import redfoxexpand.client.gui.FontRenderContext;
import net.minecraft.client.gui.FontRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FontRenderer.class)
public abstract class MixinFontRenderer {

    @Inject(
            method = "drawString(Ljava/lang/String;III)I",
            at = @At("HEAD"),
            cancellable = true
    )
    private void redfoxexpand$modifyContainerLabel(
            String text,
            int x,
            int y,
            int color,
            CallbackInfoReturnable<Integer> callback
    ) {
        FontRenderContext.AdjustedText adjusted = FontRenderContext.adjust(x, y, color);
        if (adjusted != null) {
            if (adjusted.hidden) {
                callback.setReturnValue(0);
                return;
            }
            callback.setReturnValue(((FontRenderer) (Object) this).drawString(
                    text,
                    (float) adjusted.x,
                    (float) adjusted.y,
                    adjusted.color,
                    false
            ));
        }
    }
}
