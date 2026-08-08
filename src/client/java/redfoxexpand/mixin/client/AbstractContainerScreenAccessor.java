package redfoxexpand.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("leftPos") int redfoxexpand$getLeftPos();
    @Accessor("leftPos") void redfoxexpand$setLeftPos(int value);
    @Accessor("topPos") int redfoxexpand$getTopPos();
    @Accessor("topPos") void redfoxexpand$setTopPos(int value);
    @Accessor("imageWidth") int redfoxexpand$getImageWidth();
    @Accessor("imageHeight") int redfoxexpand$getImageHeight();
    @Accessor("titleLabelX") int redfoxexpand$getTitleLabelX();
    @Accessor("titleLabelY") int redfoxexpand$getTitleLabelY();
    @Accessor("inventoryLabelX") int redfoxexpand$getInventoryLabelX();
    @Accessor("inventoryLabelY") int redfoxexpand$getInventoryLabelY();
    @Accessor("playerInventoryTitle") Component redfoxexpand$getPlayerInventoryTitle();
}
