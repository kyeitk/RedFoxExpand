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

    @Inject(method = "updateActivePotionEffects", at = @At("RETURN"))
    private void redfoxexpand$restoreConfiguredOrigin(CallbackInfo callback) {
        GuiModifierScreenAccess access = (GuiModifierScreenAccess) (Object) this;
        access.redfoxexpand$afterInventoryEffectOriginUpdate();
    }

    @ModifyVariable(
            method = "drawActivePotionEffects",
            at = @At("STORE"),
            index = 1
    )
    private int redfoxexpand$movePotionEffectsRight(int vanillaX) {
        GuiModifierScreenAccess access = (GuiModifierScreenAccess) (Object) this;
        return access.redfoxexpand$getPotionEffectsX();
    }
}
