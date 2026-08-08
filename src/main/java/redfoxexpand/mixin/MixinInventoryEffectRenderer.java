package redfoxexpand.mixin;

import net.minecraft.client.renderer.InventoryEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import redfoxexpand.client.gui.GuiModifierScreenAccess;

@Mixin(InventoryEffectRenderer.class)
public abstract class MixinInventoryEffectRenderer {

    @Inject(method = "initGui", at = @At("RETURN"))
    private void redfoxexpand$restoreConfiguredOrigin(CallbackInfo callback) {
        Object self = this;
        if (self instanceof GuiModifierScreenAccess) {
            ((GuiModifierScreenAccess) self).redfoxexpand$afterInventoryEffectOriginUpdate();
        }
    }

    @ModifyVariable(
            method = "drawActivePotionEffects",
            at = @At("STORE"),
            index = 1
    )
    private int redfoxexpand$movePotionEffectsRight(int vanillaX) {
        Object self = this;
        return self instanceof GuiModifierScreenAccess
                ? ((GuiModifierScreenAccess) self).redfoxexpand$getPotionEffectsX()
                : vanillaX;
    }
}
