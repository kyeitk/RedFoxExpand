package redfoxexpand.client.render;

import net.minecraft.util.ResourceLocation;

/**
 * A texture source resolved entirely during resource loading. Rendering only
 * asks for the already-cached frame that should be displayed at a given time.
 */
public interface GuiTexture {

    ResourceLocation textureAt(long nowMillis);

    boolean isAnimated();
}
