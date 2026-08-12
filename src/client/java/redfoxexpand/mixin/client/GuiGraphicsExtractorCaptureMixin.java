package redfoxexpand.mixin.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import redfoxexpand.client.gui.GuiResourceCapture;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorCaptureMixin {
    @Inject(
            method = "innerBlit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIIFFFFI)V",
            at = @At("HEAD"),
            require = 0
    )
    private void redfoxexpand$captureGuiResource(RenderPipeline pipeline, Identifier resource,
                                                 int x0, int x1, int y0, int y1,
                                                 float u0, float u1, float v0, float v1,
                                                 int color, CallbackInfo callback) {
        GuiResourceCapture.accept(resource);
    }
}

