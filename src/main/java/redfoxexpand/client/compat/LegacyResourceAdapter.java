package redfoxexpand.client.compat;

import redfoxexpand.client.config.GuiTextureResolver;
import redfoxexpand.client.render.GuiTexture;
import redfoxexpand.client.render.StaticGuiTexture;
import redfoxexpand.client.resource.ResourcePathResolver;
import net.minecraft.util.ResourceLocation;

/** Compatibility adapter for the former Polytone-style resource locations. */
public final class LegacyResourceAdapter implements GuiTextureResolver {

    public static final LegacyResourceAdapter INSTANCE = new LegacyResourceAdapter();

    private LegacyResourceAdapter() {
    }

    @Override
    public GuiTexture resolveStatic(String configuredPath, boolean legacyGuiAtlasId) {
        return new StaticGuiTexture(resolveTextureLocation(configuredPath, legacyGuiAtlasId));
    }

    @Override
    public GuiTexture resolveAnimation(String configuredDirectory) {
        throw new IllegalArgumentException(
                "Animation directories are only supported under assets/Kyeitk/"
        );
    }

    public static ResourceLocation resolveTextureLocation(
            String configuredPath,
            boolean guiAtlasId
    ) {
        ResourceLocation texture = new ResourceLocation(configuredPath);
        if (!guiAtlasId) {
            return texture;
        }
        return new ResourceLocation(
                texture.getResourceDomain(),
                ResourcePathResolver.LEGACY_GUI_SPRITE_ROOT
                        + texture.getResourcePath() + ".png"
        );
    }
}
