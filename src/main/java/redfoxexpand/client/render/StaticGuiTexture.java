package redfoxexpand.client.render;

import net.minecraft.util.ResourceLocation;

public final class StaticGuiTexture implements GuiTexture {

    private final ResourceLocation texture;

    public StaticGuiTexture(ResourceLocation texture) {
        if (texture == null) {
            throw new IllegalArgumentException("texture must not be null");
        }
        this.texture = texture;
    }

    @Override
    public ResourceLocation textureAt(long nowMillis) {
        return texture;
    }

    @Override
    public boolean isAnimated() {
        return false;
    }
}
