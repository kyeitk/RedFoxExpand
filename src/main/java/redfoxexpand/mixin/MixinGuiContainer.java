package redfoxexpand.mixin;

import redfoxexpand.RedFoxExpand;
import redfoxexpand.client.gui.FontRenderContext;
import redfoxexpand.client.gui.GuiModifierScreenAccess;
import redfoxexpand.client.gui.InventoryEffectGeometry;
import redfoxexpand.client.gui.ResolvedGuiModifier;
import redfoxexpand.client.gui.SlotModifier;
import redfoxexpand.client.gui.SpriteOverlay;
import redfoxexpand.client.gui.VanillaBackgroundGeometry;
import redfoxexpand.client.gui.ReactiveScreenRuntime;
import redfoxexpand.platform.forge189.Forge189Clock;
import redfoxexpand.platform.forge189.Forge189RuntimeDiagnostics;
import redfoxexpand.platform.forge189.Forge189RuntimeStateProvider;
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

    @Unique
    private ReactiveScreenRuntime redfoxexpand$reactiveRuntime;

    @Unique
    private Object redfoxexpand$playerIdentity;

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
    public void redfoxexpand$tickReactive() {
        if (redfoxexpand$reactiveRuntime != null) {
            Object player = net.minecraft.client.Minecraft.getMinecraft().thePlayer;
            redfoxexpand.reactive.runtime.RuntimeSnapshot snapshot =
                    Forge189RuntimeStateProvider.snapshot(
                            net.minecraft.client.Minecraft.getMinecraft(), width, height,
                            guiLeft, guiTop, xSize, ySize);
            if (player != redfoxexpand$playerIdentity) {
                redfoxexpand$reactiveRuntime.clear();
                redfoxexpand$reactiveRuntime = new ReactiveScreenRuntime(
                        redfoxexpand$modifier.reactiveDefinitions,
                        new Forge189RuntimeDiagnostics());
                redfoxexpand$playerIdentity = player;
                redfoxexpand$reactiveRuntime.initialize(
                        snapshot, Forge189Clock.INSTANCE.nowMillis());
                return;
            }
            redfoxexpand$reactiveRuntime.tick(
                    snapshot,
                    Forge189Clock.INSTANCE.nowMillis());
        }
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
        redfoxexpand$clearReactiveRuntime();
        xSize = redfoxexpand$baseXSize;
        ySize = redfoxexpand$baseYSize;
        guiLeft = redfoxexpand$baseGuiLeft;
        guiTop = redfoxexpand$baseGuiTop;

        GuiContainer self = (GuiContainer) (Object) this;
        redfoxexpand$modifier = RedFoxExpand.GUI_MODIFIERS.resolve(
                self, guiLeft, guiTop, xSize, ySize);
        if (redfoxexpand$modifier != null) {
            xSize += redfoxexpand$modifier.widthOffset;
            ySize += redfoxexpand$modifier.heightOffset;
            guiLeft += redfoxexpand$modifier.xOffset - redfoxexpand$modifier.widthOffset / 2;
            guiTop += redfoxexpand$modifier.yOffset - redfoxexpand$modifier.heightOffset / 2;
        }
        redfoxexpand$configuredGuiLeft = guiLeft;
        redfoxexpand$configuredOriginReady = true;
        RedFoxExpand.GUI_MODIFIERS.applyAllSlots(self, redfoxexpand$modifier);
        if (redfoxexpand$modifier != null
                && !redfoxexpand$modifier.reactiveDefinitions.isEmpty()) {
            redfoxexpand$reactiveRuntime = new ReactiveScreenRuntime(
                    redfoxexpand$modifier.reactiveDefinitions,
                    new Forge189RuntimeDiagnostics());
            redfoxexpand$playerIdentity =
                    net.minecraft.client.Minecraft.getMinecraft().thePlayer;
            redfoxexpand$reactiveRuntime.initialize(
                    Forge189RuntimeStateProvider.snapshot(
                            net.minecraft.client.Minecraft.getMinecraft(), width, height,
                            guiLeft, guiTop, xSize, ySize),
                    Forge189Clock.INSTANCE.nowMillis());
        }
    }

    @Unique
    private void redfoxexpand$clearReactiveRuntime() {
        if (redfoxexpand$reactiveRuntime != null) {
            redfoxexpand$reactiveRuntime.clear();
            redfoxexpand$reactiveRuntime = null;
        }
        redfoxexpand$playerIdentity = null;
    }

    @Inject(method = "onGuiClosed", at = @At("HEAD"))
    private void redfoxexpand$disposeReactiveRuntime(CallbackInfo callback) {
        redfoxexpand$clearReactiveRuntime();
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
                    xSize,
                    ySize,
                    width,
                    height,
                    false,
                    redfoxexpand$reactiveRuntime
            );
            redfoxexpand$modifier.renderTextLayer(
                    SpriteOverlay.Layer.UNDERLAY, guiLeft, guiTop,
                    xSize, ySize,
                    width, height, false);
        }
        int originalWidth = width;
        int originalHeight = height;
        int modifiedXSize = xSize;
        int modifiedYSize = ySize;
        try {
            // Vanilla GUI implementations are inconsistent: some use guiLeft/guiTop,
            // while others recalculate their origin from width/xSize on every frame.
            // Never expose an expanded xSize/ySize to the vanilla texture draw: 1.8.9
            // GUI UVs assume a 256px atlas, so sampling the logical extension wraps
            // the texture and draws a duplicated panel on the right/bottom.
            if (redfoxexpand$modifier != null && redfoxexpand$baseCaptured) {
                xSize = redfoxexpand$baseXSize;
                ySize = redfoxexpand$baseYSize;
                width = VanillaBackgroundGeometry.screenWidthForOrigin(
                        guiLeft,
                        redfoxexpand$baseXSize
                );
                height = VanillaBackgroundGeometry.screenHeightForOrigin(
                        guiTop,
                        redfoxexpand$baseYSize
                );
            }
            drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
        } finally {
            width = originalWidth;
            height = originalHeight;
            xSize = modifiedXSize;
            ySize = modifiedYSize;
        }
        if (redfoxexpand$modifier != null) {
            RedFoxExpand.GUI_TEXTURE_RENDERER.renderLayer(
                    redfoxexpand$modifier.sprites,
                    SpriteOverlay.Layer.BACKGROUND,
                    guiLeft,
                    guiTop,
                    xSize,
                    ySize,
                    width,
                    height,
                    false,
                    redfoxexpand$reactiveRuntime
            );
            redfoxexpand$modifier.renderTextLayer(
                    SpriteOverlay.Layer.BACKGROUND, guiLeft, guiTop,
                    xSize, ySize,
                    width, height, false);
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
                    xSize,
                    ySize,
                    width,
                    height,
                    true,
                    redfoxexpand$reactiveRuntime
            );
            redfoxexpand$modifier.renderTextLayer(
                    SpriteOverlay.Layer.FOREGROUND, guiLeft, guiTop,
                    xSize, ySize,
                    width, height, true);
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
