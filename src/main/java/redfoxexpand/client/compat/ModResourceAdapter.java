package redfoxexpand.client.compat;

import redfoxexpand.client.resource.ResourcePathResolver;
import net.minecraftforge.fml.common.Loader;

/** Selects compatibility configs only when their target Mod is loaded. */
public final class ModResourceAdapter {

    private ModResourceAdapter() {
    }

    public static boolean isApplicableConfig(String path) {
        String modId = targetModId(path);
        return modId != null && Loader.isModLoaded(modId);
    }

    public static String targetModId(String path) {
        if (!path.startsWith(ResourcePathResolver.COMPATIBILITY_ROOT)) {
            return null;
        }
        String remainder = path.substring(ResourcePathResolver.COMPATIBILITY_ROOT.length());
        int separator = remainder.indexOf('/');
        if (separator <= 0) {
            return null;
        }
        String modId = remainder.substring(0, separator);
        String nested = remainder.substring(separator + 1);
        return nested.startsWith(ResourcePathResolver.CONFIG_ROOT)
                && nested.endsWith(".json") ? modId : null;
    }
}
