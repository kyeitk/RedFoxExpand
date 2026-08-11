package redfoxexpand.client.resource;

import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import redfoxexpand.client.render.AnimatedGuiRenderer;
import redfoxexpand.client.render.AnimationPlaybackCondition;
import redfoxexpand.client.render.GuiTexture;
import redfoxexpand.client.render.StaticGuiTexture;
import redfoxexpand.core.GuiDefinition;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Resolves strict native v2/v3 TextureSpec objects through 1.7.10 ResourceManager. */
public final class NativeTextureResolver {
    private final IResourceManager manager;
    private final Map<ResourceLocation, Long> validatedPixels = new HashMap<ResourceLocation, Long>();
    private long generationPixels;

    public NativeTextureResolver(IResourceManager manager) { this.manager = manager; }

    public GuiTexture resolve(GuiDefinition.Sprite sprite) {
        ResourceLocation fallback = resolveAndRequire(sprite.texture());
        GuiDefinition.Animation animation = sprite.animation();
        if (animation == null || animation.condition() == GuiDefinition.AnimationCondition.NEVER) {
            return new StaticGuiTexture(fallback);
        }
        ResourceLocation defaultTexture = resolveAndRequire(animation.defaultTexture());
        List<AnimatedGuiRenderer.Frame> frames = new ArrayList<AnimatedGuiRenderer.Frame>();
        long animationPixels = 0L;
        for (GuiDefinition.AnimationFrame frame : animation.frames()) {
            try {
                ResourceLocation frameTexture = resolveAndRequire(frame.texture());
                animationPixels += validatedPixels.get(frameTexture).longValue();
                if (animationPixels > redfoxexpand.core.ResourceLimits.MAX_ANIMATION_PIXELS) {
                    throw new IllegalArgumentException("animation exceeds the pixel budget");
                }
                frames.add(new AnimatedGuiRenderer.Frame(
                        frameTexture, frame.durationMillis()));
            } catch (IllegalArgumentException error) {
                if (animation.missingFrameBehavior() == GuiDefinition.MissingFrameBehavior.USE_DEFAULT) {
                    frames.add(new AnimatedGuiRenderer.Frame(defaultTexture, frame.durationMillis()));
                } else if (animation.missingFrameBehavior() == GuiDefinition.MissingFrameBehavior.DISABLE) {
                    return new StaticGuiTexture(defaultTexture);
                }
            }
        }
        if (frames.isEmpty()) return new StaticGuiTexture(defaultTexture);
        return new AnimatedGuiRenderer(frames, defaultTexture, animation.loop(),
                new AnimationPlaybackCondition() {
                    @Override public boolean shouldPlay() { return true; }
                }, redfoxexpand.platform.forge1710.Forge1710Clock.INSTANCE.nowMillis());
    }

    public ResourceLocation resolveAndRequire(GuiDefinition.TextureSpec spec) {
        ResourceLocation result = resolve(spec);
        if (validatedPixels.containsKey(result)) return result;
        InputStream stream = null;
        try {
            IResource resource = manager.getResource(result);
            stream = NativeManifestScanner.limited(resource.getInputStream(),
                    redfoxexpand.core.ResourceLimits.MAX_PNG_BYTES, result.toString());
            byte[] header = new byte[24];
            int offset = 0;
            while (offset < header.length) {
                int read = stream.read(header, offset, header.length - offset);
                if (read < 0) throw new IllegalArgumentException("Incomplete PNG " + result);
                offset += read;
            }
            long pixels = pngPixels(header, result.toString());
            byte[] buffer = new byte[8192];
            while (stream.read(buffer) != -1) { }
            generationPixels += pixels;
            if (generationPixels > redfoxexpand.core.ResourceLimits.MAX_RELOAD_PIXELS) {
                throw new IllegalArgumentException("native textures exceed the reload pixel budget");
            }
            validatedPixels.put(result, Long.valueOf(pixels));
        } catch (Exception error) {
            throw new IllegalArgumentException("Missing native texture " + result, error);
        } finally {
            if (stream != null) try { stream.close(); } catch (Exception ignored) { }
        }
        return result;
    }

    private static long pngPixels(byte[] png, String label) {
        byte[] signature = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        for (int index = 0; index < signature.length; index++) {
            if (png[index] != signature[index]) throw new IllegalArgumentException("Invalid PNG " + label);
        }
        if (png[12] != 'I' || png[13] != 'H' || png[14] != 'D' || png[15] != 'R') {
            throw new IllegalArgumentException("Missing PNG IHDR " + label);
        }
        long width = ((png[16] & 0xFFL) << 24) | ((png[17] & 0xFFL) << 16)
                | ((png[18] & 0xFFL) << 8) | (png[19] & 0xFFL);
        long height = ((png[20] & 0xFFL) << 24) | ((png[21] & 0xFFL) << 16)
                | ((png[22] & 0xFFL) << 8) | (png[23] & 0xFFL);
        if (width <= 0 || height <= 0
                || width > redfoxexpand.core.ResourceLimits.MAX_IMAGE_DIMENSION
                || height > redfoxexpand.core.ResourceLimits.MAX_IMAGE_DIMENSION) {
            throw new IllegalArgumentException(label + " exceeds the image dimension budget");
        }
        long pixels = width * height;
        if (pixels > redfoxexpand.core.ResourceLimits.MAX_IMAGE_PIXELS) {
            throw new IllegalArgumentException(label + " exceeds the image pixel budget");
        }
        return pixels;
    }

    public static ResourceLocation resolve(GuiDefinition.TextureSpec spec) {
        String configured = spec.location();
        ResourceLocation id;
        if (spec.type() == GuiDefinition.ResourceType.PACK_RESOURCE) {
            if (configured.indexOf(':') >= 0) return new ResourceLocation(configured);
            String path = configured.startsWith("redfoxexpand/")
                    ? configured : "redfoxexpand/" + configured;
            return new ResourceLocation(ResourcePathResolver.LOWERCASE_NAMESPACE, path);
        }
        id = new ResourceLocation(configured);
        if (spec.type() == GuiDefinition.ResourceType.GUI_SPRITE) {
            return new ResourceLocation(id.getResourceDomain(),
                    "textures/gui/sprites/" + id.getResourcePath() + ".png");
        }
        return id;
    }
}
