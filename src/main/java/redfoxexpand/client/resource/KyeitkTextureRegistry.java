package redfoxexpand.client.resource;

import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Owns the dynamic textures created for one immutable resource snapshot. */
public final class KyeitkTextureRegistry implements AutoCloseable {

    private static final AtomicInteger NEXT_GENERATION = new AtomicInteger();

    private final TextureManager textureManager;
    private final int generation = NEXT_GENERATION.incrementAndGet();
    private final Map<String, ResourceLocation> cached =
            new LinkedHashMap<String, ResourceLocation>();
    private final List<ResourceLocation> owned = new ArrayList<ResourceLocation>();
    private long decodedPixels;

    public KyeitkTextureRegistry(TextureManager textureManager) {
        this.textureManager = textureManager;
    }

    public ResourceLocation load(
            String path,
            KyeitkResourceScanner.ResourceFile resource
    ) {
        String normalized = ResourcePathResolver.normalizeRelativePath(path);
        ResourceLocation existing = cached.get(normalized);
        if (existing != null) {
            return existing;
        }
        if (!normalized.toLowerCase(java.util.Locale.ROOT).endsWith(".png")) {
            throw new IllegalArgumentException("GUI texture must be a PNG: " + path);
        }

        InputStream stream = null;
        try {
            stream = resource.open();
            BufferedImage image = ImageIO.read(stream);
            if (image == null) {
                throw new IllegalArgumentException("Could not decode PNG: " + resource);
            }
            long pixels = ResourceLimits.imagePixels(image, resource.toString());
            if (decodedPixels + pixels > ResourceLimits.MAX_GENERATION_PIXELS) {
                throw new IllegalArgumentException("Dynamic texture generation budget exceeded");
            }
            ResourceLocation runtime = new ResourceLocation(
                    "redfoxexpand",
                    "kyeitk_runtime/" + generation + "/" + normalized
            );
            if (!textureManager.loadTexture(runtime, new DynamicTexture(image))) {
                throw new IllegalArgumentException("Texture manager rejected PNG: " + resource);
            }
            cached.put(normalized, runtime);
            owned.add(runtime);
            decodedPixels += pixels;
            return runtime;
        } catch (Exception error) {
            throw new IllegalArgumentException("Could not load Kyeitk texture " + resource, error);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Override
    public void close() {
        for (ResourceLocation texture : owned) {
            textureManager.deleteTexture(texture);
        }
        owned.clear();
        cached.clear();
        decodedPixels = 0L;
    }
}
