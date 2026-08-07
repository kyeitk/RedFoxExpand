package redfoxexpand.client.config;

import redfoxexpand.client.render.GuiTexture;

/** Resolves config paths into textures cached during the resource reload. */
public interface GuiTextureResolver {

    GuiTexture resolveStatic(String configuredPath, boolean legacyGuiAtlasId);

    GuiTexture resolveAnimation(String configuredDirectory);
}
