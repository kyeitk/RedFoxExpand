package redfoxexpand.mixin;

import redfoxexpand.client.gui.SlotBaseAccess;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Slot.class)
public abstract class MixinSlot implements SlotBaseAccess {

    @Shadow
    public int xDisplayPosition;

    @Shadow
    public int yDisplayPosition;

    @Unique
    private boolean redfoxexpand$baseCaptured;

    @Unique
    private int redfoxexpand$baseX;

    @Unique
    private int redfoxexpand$baseY;

    @Override
    public void redfoxexpand$captureBase() {
        if (!redfoxexpand$baseCaptured) {
            redfoxexpand$baseX = xDisplayPosition;
            redfoxexpand$baseY = yDisplayPosition;
            redfoxexpand$baseCaptured = true;
        }
    }

    @Override
    public void redfoxexpand$resetToBase() {
        if (redfoxexpand$baseCaptured) {
            xDisplayPosition = redfoxexpand$baseX;
            yDisplayPosition = redfoxexpand$baseY;
        }
    }

    @Override
    public int redfoxexpand$getBaseX() {
        return redfoxexpand$baseCaptured ? redfoxexpand$baseX : xDisplayPosition;
    }

    @Override
    public int redfoxexpand$getBaseY() {
        return redfoxexpand$baseCaptured ? redfoxexpand$baseY : yDisplayPosition;
    }
}
