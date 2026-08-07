package redfoxexpand.mixin;

import redfoxexpand.RedFoxExpand;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Container.class)
public abstract class MixinContainer {

    @Inject(method = "addSlotToContainer", at = @At("RETURN"))
    private void redfoxexpand$onAddSlot(
            Slot input,
            CallbackInfoReturnable<Slot> callback
    ) {
        RedFoxExpand.GUI_MODIFIERS.onSlotAdded(
                (Container) (Object) this,
                callback.getReturnValue()
        );
    }
}
