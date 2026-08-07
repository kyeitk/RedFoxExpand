package redfoxexpand.client.gui;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

final class ClassNameMatcher {

    private ClassNameMatcher() {
    }

    static boolean matches(String configuredName, Class<?> runtimeClass) {
        return matches(configuredName, runtimeClass, ClassMatchMode.EXACT);
    }

    static boolean matches(
            String configuredName,
            Class<?> runtimeClass,
            ClassMatchMode matchMode
    ) {
        if (matchMode == ClassMatchMode.ASSIGNABLE) {
            return matchesHierarchy(configuredName, runtimeClass, new HashSet<Class<?>>());
        }
        return matchesExactClass(configuredName, runtimeClass);
    }

    private static boolean matchesHierarchy(
            String configuredName,
            Class<?> runtimeClass,
            Set<Class<?>> visited
    ) {
        if (runtimeClass == null || !visited.add(runtimeClass)) {
            return false;
        }
        if (matchesExactClass(configuredName, runtimeClass)) {
            return true;
        }
        for (Class<?> implementedInterface : runtimeClass.getInterfaces()) {
            if (matchesHierarchy(configuredName, implementedInterface, visited)) {
                return true;
            }
        }
        return matchesHierarchy(configuredName, runtimeClass.getSuperclass(), visited);
    }

    private static boolean matchesExactClass(String configuredName, Class<?> runtimeClass) {
        boolean allowSimpleName = isSimpleName(configuredName);
        String compatibleName = legacyCompatibilityAlias(configuredName);
        if (matchesRuntimeName(configuredName, runtimeClass, allowSimpleName)
                || matchesRuntimeName(compatibleName, runtimeClass, allowSimpleName)) {
            return true;
        }

        // In an obfuscated 1.8.9 client JSON still contains MCP/SRG class names.
        String obfuscatedConfiguredName = remap(compatibleName, "unmap");
        if (obfuscatedConfiguredName != null
                && (obfuscatedConfiguredName.equals(runtimeClass.getName())
                || (allowSimpleName
                && simpleName(obfuscatedConfiguredName).equals(runtimeClass.getSimpleName())))) {
            return true;
        }

        // Mapping the runtime name in the other direction also preserves support
        // for simple MCP class names such as "GuiCrafting" in production.
        String deobfuscatedRuntimeName = remap(runtimeClass.getName(), "map");
        return deobfuscatedRuntimeName != null
                && (compatibleName.equals(deobfuscatedRuntimeName)
                || (allowSimpleName
                && simpleName(compatibleName).equals(simpleName(deobfuscatedRuntimeName))));
    }

    private static boolean matchesRuntimeName(
            String configuredName,
            Class<?> runtimeClass,
            boolean allowSimpleName
    ) {
        return configuredName.equals(runtimeClass.getName())
                || (allowSimpleName
                && simpleName(configuredName).equals(runtimeClass.getSimpleName()));
    }

    private static boolean isSimpleName(String configuredName) {
        return configuredName.indexOf('.') < 0;
    }

    private static String legacyCompatibilityAlias(String configuredName) {
        // Modern Polytone packs target the player inventory through InventoryMenu.
        // Minecraft 1.8.9 exposes the same logical container as ContainerPlayer.
        if ("net.minecraft.world.inventory.InventoryMenu".equals(configuredName)
                || "InventoryMenu".equals(configuredName)) {
            return "net.minecraft.inventory.ContainerPlayer";
        }
        return configuredName;
    }

    private static String remap(String className, String methodName) {
        try {
            Class<?> remapperClass = Class.forName(
                    "net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper"
            );
            Field instanceField = remapperClass.getField("INSTANCE");
            Object remapper = instanceField.get(null);
            Method method = remapperClass.getMethod(methodName, String.class);
            Object result = method.invoke(remapper, className.replace('.', '/'));
            return result == null ? null : result.toString().replace('/', '.');
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String simpleName(String name) {
        int separator = name.lastIndexOf('.');
        return separator < 0 ? name : name.substring(separator + 1);
    }
}
