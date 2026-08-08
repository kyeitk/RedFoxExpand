package redfoxexpand.client.gui;

import redfoxexpand.platform.forge1710.Forge1710ClassAliases;
import redfoxexpand.platform.forge1710.Forge1710ClassRemapper;
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
        String compatibleName = Forge1710ClassAliases.alias(configuredName);
        if (matchesRuntimeName(configuredName, runtimeClass, allowSimpleName)
                || matchesRuntimeName(compatibleName, runtimeClass, allowSimpleName)) {
            return true;
        }

        // In an obfuscated 1.7.10 client JSON still contains MCP/SRG class names.
        String obfuscatedConfiguredName = Forge1710ClassRemapper.remap(
                compatibleName,
                "unmap"
        );
        if (obfuscatedConfiguredName != null
                && (obfuscatedConfiguredName.equals(runtimeClass.getName())
                || (allowSimpleName
                && simpleName(obfuscatedConfiguredName).equals(runtimeClass.getSimpleName())))) {
            return true;
        }

        // Mapping the runtime name in the other direction also preserves support
        // for simple MCP class names such as "GuiCrafting" in production.
        String deobfuscatedRuntimeName = Forge1710ClassRemapper.remap(
                runtimeClass.getName(),
                "map"
        );
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

    private static String simpleName(String name) {
        int separator = name.lastIndexOf('.');
        return separator < 0 ? name : name.substring(separator + 1);
    }
}
