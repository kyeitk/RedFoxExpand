package redfoxexpand.client.resource;

import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Tracks only SimpleTexture entries first created by native v2/v3 Sprite rendering. */
public final class NativeTextureCache {
    private static final Set<ResourceLocation> OWNED = new LinkedHashSet<ResourceLocation>();

    private NativeTextureCache() { }

    public static void bind(TextureManager textureManager, ResourceLocation texture) {
        if (textureManager.getTexture(texture) == null) claimIfAbsent(texture, false);
        textureManager.bindTexture(texture);
    }

    /** Called at TextureManager reload HEAD, before vanilla iterates its persistent cache map. */
    public static int evictOwned(TextureManager textureManager,
                                 Map<ResourceLocation, ITextureObject> textureObjects) {
        return evictOwned(textureObjects, new TextureDeleter() {
            @Override
            public void delete(ResourceLocation texture) {
                textureManager.deleteTexture(texture);
            }
        });
    }

    static int evictOwned(Map<ResourceLocation, ITextureObject> textureObjects,
                          TextureDeleter deleter) {
        int removed = 0;
        for (ResourceLocation texture : drainOwnedForReload()) {
            ITextureObject object = textureObjects.get(texture);
            // bindTexture creates exactly SimpleTexture for an absent ID. If another subsystem
            // replaced the entry, it now owns that object and RedFoxExpand must not remove it.
            if (!(object instanceof SimpleTexture)) continue;
            try {
                deleter.delete(texture);
            } finally {
                // Minecraft 1.8.9 deleteTexture deletes only the GL ID; it does not remove the
                // map entry, so vanilla would otherwise reload this old pack path immediately.
                textureObjects.remove(texture);
            }
            removed++;
        }
        return removed;
    }

    interface TextureDeleter {
        void delete(ResourceLocation texture);
    }

    static synchronized boolean claimIfAbsent(ResourceLocation texture, boolean alreadyLoaded) {
        return !alreadyLoaded && OWNED.add(texture);
    }

    static synchronized List<ResourceLocation> drainOwnedForReload() {
        List<ResourceLocation> result = new ArrayList<ResourceLocation>(OWNED);
        OWNED.clear();
        return result;
    }
}
