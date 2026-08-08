package redfoxexpand.client.resource;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import redfoxexpand.RedFoxExpand;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Discovers API v2 configs through the native lowercase resource domain. */
public final class NativeManifestScanner {

    public static final ResourceLocation MANIFEST = new ResourceLocation(
            ResourcePathResolver.LOWERCASE_NAMESPACE,
            "redfoxexpand/index.json"
    );

    private NativeManifestScanner() {
    }

    public static List<ResourceLocation> findConfigs(IResourceManager manager) {
        Set<ResourceLocation> result = new LinkedHashSet<ResourceLocation>();
        try {
            List<IResource> manifests = manager.getAllResources(MANIFEST);
            for (IResource resource : manifests) {
                InputStream stream = null;
                try {
                    stream = ResourceLimits.limited(
                            resource.getInputStream(),
                            ResourceLimits.MAX_CONFIG_BYTES,
                            MANIFEST.toString()
                    );
                    JsonObject json = new JsonParser().parse(new InputStreamReader(
                            stream,
                            StandardCharsets.UTF_8
                    )).getAsJsonObject();
                    int apiVersion = json.has("api_version")
                            ? json.get("api_version").getAsInt()
                            : 0;
                    if (apiVersion != 2) {
                        throw new IllegalArgumentException(
                                "Native Kyeitk manifest api_version must be 2"
                        );
                    }
                    JsonArray configs = json.getAsJsonArray("configs");
                    if (configs == null || configs.size() > 1024) {
                        throw new IllegalArgumentException(
                                "Native Kyeitk manifest requires at most 1024 configs"
                        );
                    }
                    for (JsonElement element : configs) {
                        String path = ResourcePathResolver.normalizeRelativePath(
                                element.getAsString()
                        );
                        if (!path.startsWith("redfoxexpand/config/")
                                || !path.endsWith(".json")) {
                            throw new IllegalArgumentException(
                                    "Native config must be under redfoxexpand/config/: " + path
                            );
                        }
                        result.add(new ResourceLocation(
                                ResourcePathResolver.LOWERCASE_NAMESPACE,
                                path
                        ));
                    }
                } catch (Exception error) {
                    RedFoxExpand.LOGGER.error("Invalid native Kyeitk v2 manifest", error);
                } finally {
                    if (stream != null) {
                        stream.close();
                    }
                }
            }
        } catch (java.io.FileNotFoundException ignored) {
            // Native API v2 is optional.
        } catch (java.io.IOException ignored) {
            // 1.7.10 reports a missing resource as a generic IOException.
        }
        return new ArrayList<ResourceLocation>(result);
    }
}
