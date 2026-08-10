package redfoxexpand.client.resource;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import redfoxexpand.RedFoxExpand;
import redfoxexpand.core.ResourceLimits;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Discovers native lowercase Schema v2/v3 configs and retains their source pack. */
public final class NativeManifestScanner {
    public static final ResourceLocation MANIFEST = new ResourceLocation(
            ResourcePathResolver.LOWERCASE_NAMESPACE, "redfoxexpand/index.json");

    private NativeManifestScanner() { }

    public static List<ConfigRef> findConfigs(IResourceManager manager) {
        List<ConfigRef> result = new ArrayList<ConfigRef>();
        try {
            List<IResource> manifests = manager.getAllResources(MANIFEST);
            for (int priority = 0; priority < manifests.size(); priority++) {
                IResource resource = manifests.get(priority);
                try {
                    parseManifest(resource, priority, result);
                } catch (Exception error) {
                    RedFoxExpand.LOGGER.error("Invalid native Schema v2/v3 manifest from {}",
                            resource.getResourcePackName(), error);
                }
            }
        } catch (IOException ignored) {
            // Native API is optional; Minecraft 1.8.9 reports absence as IOException.
        }
        return result;
    }

    private static void parseManifest(IResource resource, int priority,
                                      List<ConfigRef> result) throws IOException {
        InputStream stream = limited(resource.getInputStream(), ResourceLimits.MAX_JSON_BYTES,
                MANIFEST.toString());
        try {
            JsonElement parsed = new JsonParser().parse(new InputStreamReader(stream, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) throw new IllegalArgumentException("manifest root must be an object");
            JsonObject json = parsed.getAsJsonObject();
            Set<String> unknown = new HashSet<String>();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) unknown.add(entry.getKey());
            unknown.remove("api_version"); unknown.remove("configs");
            if (!unknown.isEmpty()) throw new IllegalArgumentException("unknown manifest fields: " + unknown);
            int apiVersion = json.has("api_version") ? json.get("api_version").getAsInt() : 0;
            if (apiVersion != 2 && apiVersion != 3) {
                throw new IllegalArgumentException("manifest api_version must be 2 or 3");
            }
            JsonArray configs = json.getAsJsonArray("configs");
            if (configs == null || configs.size() > ResourceLimits.MAX_MANIFEST_CONFIGS) {
                throw new IllegalArgumentException("manifest configs exceeds " + ResourceLimits.MAX_MANIFEST_CONFIGS);
            }
            Set<ResourceLocation> unique = new LinkedHashSet<ResourceLocation>();
            for (JsonElement element : configs) {
                if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                    throw new IllegalArgumentException("manifest config entries must be strings");
                }
                String configured = ResourceLimits.safePath(element.getAsString(), "manifest config");
                ResourceLocation location = configured.indexOf(':') >= 0
                        ? new ResourceLocation(configured)
                        : new ResourceLocation(ResourcePathResolver.LOWERCASE_NAMESPACE, configured);
                if (!ResourcePathResolver.LOWERCASE_NAMESPACE.equals(location.getResourceDomain())
                        || !location.getResourcePath().startsWith("redfoxexpand/config/")
                        || !location.getResourcePath().endsWith(".json")) {
                    throw new IllegalArgumentException("config must be under kyeitk:redfoxexpand/config/: " + location);
                }
                if (unique.add(location)) {
                    result.add(new ConfigRef(apiVersion, location, resource.getResourcePackName(), priority));
                }
            }
        } finally {
            stream.close();
        }
    }

    public static IResource samePackResource(IResourceManager manager, ConfigRef ref) throws IOException {
        for (IResource resource : manager.getAllResources(ref.location)) {
            if (ref.sourcePack.equals(resource.getResourcePackName())) return resource;
        }
        throw new IOException("Missing same-pack config " + ref.location + " from " + ref.sourcePack);
    }

    public static InputStream limited(InputStream source, final long maximum, final String label) {
        return new FilterInputStream(source) {
            private long count;
            @Override public int read() throws IOException {
                int value = super.read(); if (value >= 0) consume(1); return value;
            }
            @Override public int read(byte[] buffer, int offset, int length) throws IOException {
                int read = super.read(buffer, offset, length); if (read > 0) consume(read); return read;
            }
            private void consume(int amount) throws IOException {
                count += amount;
                if (count > maximum) throw new IOException(label + " exceeds " + maximum + " bytes");
            }
        };
    }

    public static final class ConfigRef {
        public final int apiVersion;
        public final ResourceLocation location;
        public final String sourcePack;
        public final int sourcePriority;
        ConfigRef(int apiVersion, ResourceLocation location, String sourcePack, int sourcePriority) {
            this.apiVersion = apiVersion; this.location = location;
            this.sourcePack = sourcePack; this.sourcePriority = sourcePriority;
        }
        @Override public String toString() { return location + " from " + sourcePack; }
    }
}
