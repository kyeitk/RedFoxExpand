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
    private int redfoxexpand$appliedX;

    @Unique
    private int redfoxexpand$appliedY;

    @Override
    public void redfoxexpand$removeAppliedDelta() {
        xDisplayPosition -= redfoxexpand$appliedX;
        yDisplayPosition -= redfoxexpand$appliedY;
        redfoxexpand$appliedX = 0;
        redfoxexpand$appliedY = 0;
    }

    @Override
    public void redfoxexpand$recordAppliedDelta(int x, int y) {
        redfoxexpand$appliedX += x;
        redfoxexpand$appliedY += y;
    }

    @Override
    public int redfoxexpand$getBaseX() {
        return xDisplayPosition - redfoxexpand$appliedX;
    }

    @Override
    public int redfoxexpand$getBaseY() {
        return yDisplayPosition - redfoxexpand$appliedY;
    }
}
