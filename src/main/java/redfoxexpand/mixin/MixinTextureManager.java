package redfoxexpand.mixin;

import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import redfoxexpand.client.resource.NativeTextureCache;

import java.util.Map;

@Mixin(TextureManager.class)
public abstract class MixinTextureManager {
    @Shadow
    @Final
    private Map<ResourceLocation, ITextureObject> mapTextureObjects;

    @Inject(method = "onResourceManagerReload", at = @At("HEAD"))
    private void redfoxexpand$evictPreviousNativeTextures(
            IResourceManager resourceManager,
            CallbackInfo callback
    ) {
        NativeTextureCache.evictOwned(
                (TextureManager) (Object) this,
                mapTextureObjects
        );
    }
}
