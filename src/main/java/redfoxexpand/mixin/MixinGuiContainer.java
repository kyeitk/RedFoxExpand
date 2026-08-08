package redfoxexpand.mixin;

import redfoxexpand.RedFoxExpand;
import redfoxexpand.client.gui.FontRenderContext;
import redfoxexpand.client.gui.GuiModifierScreenAccess;
import redfoxexpand.client.gui.InventoryEffectGeometry;
import redfoxexpand.client.gui.ResolvedGuiModifier;
import redfoxexpand.client.gui.SlotModifier;
import redfoxexpand.client.gui.SpriteOverlay;
import redfoxexpand.platform.forge1710.Forge1710BackgroundGeometry;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.lwjgl.opengl.GL11;

@Mixin(GuiContainer.class)
public abstract class MixinGuiContainer extends GuiScreen implements GuiModifierScreenAccess {

    @Shadow
    protected int guiLeft;

    @Shadow
    protected int guiTop;

    @Shadow
    protected int xSize;

    @Shadow
    protected int ySize;

    @Shadow
    public Container inventorySlots;

    @Shadow
    private Slot theSlot;

    @Shadow
    protected abstract void drawGuiContainerForegroundLayer(int mouseX, int mouseY);

    @Shadow
    protected abstract void drawGuiContainerBackgroundLayer(
            float partialTicks,
            int mouseX,
            int mouseY
    );

    @Unique
    private ResolvedGuiModifier redfoxexpand$modifier;

    @Unique
    private boolean redfoxexpand$baseCaptured;

    @Unique
    private boolean redfoxexpand$initializing;

    @Unique
    private int redfoxexpand$baseXSize;

    @Unique
    private int redfoxexpand$baseYSize;

    @Unique
    private int redfoxexpand$baseGuiLeft;

    @Unique
    private int redfoxexpand$baseGuiTop;

    @Unique
    private int redfoxexpand$configuredGuiLeft;

    @Unique
    private boolean redfoxexpand$configuredOriginReady;

    @Inject(method = "initGui", at = @At("HEAD"))
    private void redfoxexpand$resetSizeBeforeInit(CallbackInfo callback) {
        redfoxexpand$initializing = true;
        redfoxexpand$configuredOriginReady = false;
        if (redfoxexpand$baseCaptured) {
            xSize = redfoxexpand$baseXSize;
            ySize = redfoxexpand$baseYSize;
        }
    }

    @Override
    public ResolvedGuiModifier redfoxexpand$getModifier() {
        return redfoxexpand$modifier;
    }

    @Override
    public void redfoxexpand$onPostInit() {
        redfoxexpand$baseXSize = xSize;
        redfoxexpand$baseYSize = ySize;
        redfoxexpand$baseGuiLeft = guiLeft;
        redfoxexpand$baseGuiTop = guiTop;
        redfoxexpand$baseCaptured = true;
        redfoxexpand$initializing = false;
        redfoxexpand$resolveAndApply();
    }

    @Override
    public void redfoxexpand$afterInventoryEffectOriginUpdate() {
        guiLeft = InventoryEffectGeometry.stableGuiLeft(
                width,
                xSize,
                redfoxexpand$configuredOriginReady,
                redfoxexpand$configuredGuiLeft
        );
    }

    @Override
    public int redfoxexpand$getPotionEffectsX() {
        return InventoryEffectGeometry.rightPotionEffectsX(guiLeft, xSize, width);
    }

    @Override
    public void redfoxexpand$refreshModifier() {
        if (!redfoxexpand$baseCaptured) {
            redfoxexpand$baseXSize = xSize;
            redfoxexpand$baseYSize = ySize;
            redfoxexpand$baseGuiLeft = guiLeft;
            redfoxexpand$baseGuiTop = guiTop;
            redfoxexpand$baseCaptured = true;
        }
        redfoxexpand$resolveAndApply();
    }

    @Unique
    private void redfoxexpand$resolveAndApply() {
        xSize = redfoxexpand$baseXSize;
        ySize = redfoxexpand$baseYSize;
        guiLeft = redfoxexpand$baseGuiLeft;
        guiTop = redfoxexpand$baseGuiTop;

        GuiContainer self = (GuiContainer) (Object) this;
        redfoxexpand$modifier = RedFoxExpand.GUI_MODIFIERS.resolve(self);
        if (redfoxexpand$modifier != null) {
            xSize += redfoxexpand$modifier.widthOffset;
            ySize += redfoxexpand$modifier.heightOffset;
            guiLeft += redfoxexpand$modifier.xOffset - redfoxexpand$modifier.widthOffset / 2;
            guiTop += redfoxexpand$modifier.yOffset - redfoxexpand$modifier.heightOffset / 2;
        }
        redfoxexpand$configuredGuiLeft = guiLeft;
        redfoxexpand$configuredOriginReady = true;
        RedFoxExpand.GUI_MODIFIERS.applyAllSlots(self, redfoxexpand$modifier);
    }

    @Redirect(
            method = "drawScreen",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/inventory/GuiContainer;drawGuiContainerBackgroundLayer(FII)V"
            )
    )
    private void redfoxexpand$renderBackground(
            GuiContainer ignored,
            float partialTicks,
            int mouseX,
            int mouseY
    ) {
        if (redfoxexpand$modifier != null) {
            RedFoxExpand.GUI_TEXTURE_RENDERER.renderLayer(
                    redfoxexpand$modifier.sprites,
                    SpriteOverlay.Layer.UNDERLAY,
                    guiLeft,
                    guiTop,
                    width,
                    height,
                    false
            );
        }
        int originalGuiLeft = guiLeft;
        int originalGuiTop = guiTop;
        int modifiedXSize = xSize;
        int modifiedYSize = ySize;
        boolean translated = false;
        try {
            // Keep width/height real. Vanilla implementations may use either
            // guiLeft/guiTop or (width - xSize) / 2; setting both logical origins
            // to the centered base and translating the matrix handles both forms.
            if (redfoxexpand$modifier != null && redfoxexpand$baseCaptured) {
                xSize = redfoxexpand$baseXSize;
                ySize = redfoxexpand$baseYSize;
                int centeredLeft = Forge1710BackgroundGeometry.centeredLeft(
                        width,
                        redfoxexpand$baseXSize
                );
                int centeredTop = Forge1710BackgroundGeometry.centeredTop(
                        height,
                        redfoxexpand$baseYSize
                );
                guiLeft = centeredLeft;
                guiTop = centeredTop;
                GL11.glPushMatrix();
                GL11.glTranslatef(
                        Forge1710BackgroundGeometry.translation(originalGuiLeft, centeredLeft),
                        Forge1710BackgroundGeometry.translation(originalGuiTop, centeredTop),
                        0.0F
                );
                translated = true;
            }
            drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
        } finally {
            if (translated) {
                GL11.glPopMatrix();
            }
            guiLeft = originalGuiLeft;
            guiTop = originalGuiTop;
            xSize = modifiedXSize;
            ySize = modifiedYSize;
        }
        if (redfoxexpand$modifier != null) {
            RedFoxExpand.GUI_TEXTURE_RENDERER.renderLayer(
                    redfoxexpand$modifier.sprites,
                    SpriteOverlay.Layer.BACKGROUND,
                    guiLeft,
                    guiTop,
                    width,
                    height,
                    false
            );
        }
    }

    @Redirect(
            method = "drawScreen",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/inventory/GuiContainer;drawGuiContainerForegroundLayer(II)V"
            )
    )
    private void redfoxexpand$renderForeground(
            GuiContainer ignored,
            int mouseX,
            int mouseY
    ) {
        FontRenderContext.begin(redfoxexpand$modifier);
        try {
            drawGuiContainerForegroundLayer(mouseX, mouseY);
        } finally {
            FontRenderContext.end();
        }
        if (redfoxexpand$modifier != null) {
            // GuiContainer has translated the matrix to guiLeft/guiTop here.
            RedFoxExpand.GUI_TEXTURE_RENDERER.renderLayer(
                    redfoxexpand$modifier.sprites,
                    SpriteOverlay.Layer.FOREGROUND,
                    guiLeft,
                    guiTop,
                    width,
                    height,
                    true
            );
            redfoxexpand$modifier.renderForegroundText();
        }
    }

    @Inject(
            method = "drawScreen",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/inventory/GuiContainer;drawGradientRect(IIIIII)V",
                    shift = At.Shift.AFTER
            )
    )
    private void redfoxexpand$renderSlotHighlight(
            int mouseX,
            int mouseY,
            float partialTicks,
            CallbackInfo callback
    ) {
        GuiContainer self = (GuiContainer) (Object) this;
        Slot slot = theSlot;
        SlotModifier modifier = RedFoxExpand.GUI_MODIFIERS.highlightFor(
                self,
                redfoxexpand$modifier,
                slot
        );
        if (modifier != null) {
            drawGradientRect(
                    slot.xDisplayPosition,
                    slot.yDisplayPosition,
                    slot.xDisplayPosition + 16,
                    slot.yDisplayPosition + 16,
                    modifier.firstHighlightColor(),
                    modifier.secondHighlightColor()
            );
        }
    }
}
