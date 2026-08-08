package redfoxexpand.client.resource;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import redfoxexpand.client.compat.LegacyResourceAdapter;
import redfoxexpand.client.config.AnimationDefinition;
import redfoxexpand.client.config.GuiTextureResolver;
import redfoxexpand.client.render.AnimatedGuiRenderer;
import redfoxexpand.client.render.AnimationPlaybackCondition;
import redfoxexpand.client.render.GuiTexture;
import redfoxexpand.client.render.StaticGuiTexture;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Resolves API v2 resources without requiring a backing directory or ZIP. */
public final class NativeResourcePathResolver implements GuiTextureResolver {

    private final IResourceManager manager;

    public NativeResourcePathResolver(IResourceManager manager) {
        this.manager = manager;
    }

    @Override
    public GuiTexture resolveStatic(String configuredPath, boolean legacyGuiAtlasId) {
        ResourceLocation location = nativeLocation(configuredPath);
        if (location == null) {
            location = LegacyResourceAdapter.resolveTextureLocation(
                    configuredPath,
                    legacyGuiAtlasId
            );
        } else if (legacyGuiAtlasId) {
            location = new ResourceLocation(
                    location.getResourceDomain(),
                    ResourcePathResolver.LEGACY_GUI_SPRITE_ROOT
                            + location.getResourcePath() + ".png"
            );
        }
        requireResource(location);
        return new StaticGuiTexture(location);
    }

    @Override
    public GuiTexture resolveAnimation(String configuredDirectory) {
        ResourceLocation directory = requireNativeLocation(configuredDirectory);
        String path = trimSlash(directory.getResourcePath());
        AnimationDefinition definition = readAnimation(new ResourceLocation(
                directory.getResourceDomain(),
                path + "/" + ResourcePathResolver.ANIMATION_FILE
        ));
        String defaultPath = definition.defaultTexture.isEmpty()
                ? path + ".png"
                : animationPath(path, definition.defaultTexture);
        ResourceLocation defaultTexture = nativeTexture(directory, defaultPath);

        List<AnimatedGuiRenderer.Frame> frames = new ArrayList<AnimatedGuiRenderer.Frame>();
        for (AnimationDefinition.Frame frame : definition.frames) {
            try {
                frames.add(new AnimatedGuiRenderer.Frame(
                        nativeTexture(directory, animationPath(path, frame.texture)),
                        frame.durationMillis
                ));
            } catch (IllegalArgumentException error) {
                if (definition.missingFrameBehavior
                        == AnimationDefinition.MissingFrameBehavior.USE_DEFAULT) {
                    frames.add(new AnimatedGuiRenderer.Frame(
                            defaultTexture,
                            frame.durationMillis
                    ));
                } else if (definition.missingFrameBehavior
                        == AnimationDefinition.MissingFrameBehavior.DISABLE) {
                    return new StaticGuiTexture(defaultTexture);
                }
            }
        }
        if (frames.isEmpty()) {
            return new StaticGuiTexture(defaultTexture);
        }
        final boolean play = "always".equals(definition.playbackCondition);
        return new AnimatedGuiRenderer(
                frames,
                defaultTexture,
                definition.loop,
                new AnimationPlaybackCondition() {
                    @Override
                    public boolean shouldPlay() {
                        return play;
                    }
                },
                System.currentTimeMillis()
        );
    }

    private AnimationDefinition readAnimation(ResourceLocation location) {
        InputStream stream = null;
        try {
            stream = ResourceLimits.limited(
                    manager.getResource(location).getInputStream(),
                    ResourceLimits.MAX_ANIMATION_BYTES,
                    location.toString()
            );
            JsonObject json = new JsonParser().parse(new InputStreamReader(
                    stream,
                    StandardCharsets.UTF_8
            )).getAsJsonObject();
            return AnimationDefinition.parse(json);
        } catch (Exception error) {
            throw new IllegalArgumentException("Invalid native animation " + location, error);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private ResourceLocation nativeTexture(ResourceLocation directory, String path) {
        ResourceLocation location = new ResourceLocation(
                directory.getResourceDomain(),
                ResourcePathResolver.normalizeRelativePath(path)
        );
        requireResource(location);
        return location;
    }

    private void requireResource(ResourceLocation location) {
        InputStream stream = null;
        try {
            IResource resource = manager.getResource(location);
            stream = resource.getInputStream();
        } catch (Exception error) {
            throw new IllegalArgumentException("Missing native resource " + location, error);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static ResourceLocation requireNativeLocation(String path) {
        ResourceLocation location = nativeLocation(path);
        if (location == null) {
            throw new IllegalArgumentException("Animation must use the Kyeitk domain: " + path);
        }
        return location;
    }

    private static ResourceLocation nativeLocation(String configuredPath) {
        String trimmed = configuredPath.trim();
        int separator = trimmed.indexOf(':');
        if (separator < 0) {
            return new ResourceLocation(
                    ResourcePathResolver.LOWERCASE_NAMESPACE,
                    ResourcePathResolver.normalizeRelativePath(trimmed)
            );
        }
        String domain = trimmed.substring(0, separator);
        if (!ResourcePathResolver.PHYSICAL_NAMESPACE.equalsIgnoreCase(domain)) {
            return null;
        }
        return new ResourceLocation(
                ResourcePathResolver.LOWERCASE_NAMESPACE,
                ResourcePathResolver.normalizeRelativePath(trimmed.substring(separator + 1))
        );
    }

    private static String trimSlash(String path) {
        return path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    private static String animationPath(String directory, String configured) {
        String trimmed = configured.trim();
        int separator = trimmed.indexOf(':');
        if (separator >= 0) {
            String domain = trimmed.substring(0, separator);
            if (!ResourcePathResolver.PHYSICAL_NAMESPACE.equalsIgnoreCase(domain)) {
                throw new IllegalArgumentException(
                        "Native animation frame must use the Kyeitk domain: " + configured
                );
            }
            return trimmed.substring(separator + 1);
        }
        return trimmed.startsWith("textures/") ? trimmed : directory + "/" + trimmed;
    }
}
