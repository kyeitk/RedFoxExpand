package redfoxexpand.client.resource;

import redfoxexpand.RedFoxExpand;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.util.ResourceLocation;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Minecraft 1.8.9 has no public list-resources API. This scanner discovers the
 * enabled IResourcePack instances from the reload manager, enumerates their
 * directory/ZIP entries, then lets IResourceManager choose the winning resource.
 */
public final class ResourcePackScanner {

    private static final String PREFIX = ResourcePathResolver.LEGACY_CONFIG_ROOT;

    private ResourcePackScanner() {
    }

    public static List<ResourceLocation> findGuiModifiers(SimpleReloadableResourceManager manager) {
        Set<IResourcePack> packs = Collections.newSetFromMap(new IdentityHashMap<IResourcePack, Boolean>());
        collectResourcePacks(manager, packs);

        Set<ResourceLocation> locations = new LinkedHashSet<ResourceLocation>();
        Set<File> scannedSources = new LinkedHashSet<File>();
        for (IResourcePack pack : packs) {
            File source = findPackSource(pack);
            if (source == null) {
                RedFoxExpand.LOGGER.debug("Cannot enumerate resource pack {} (no backing file)", pack.getPackName());
                continue;
            }

            try {
                source = source.getCanonicalFile();
            } catch (Exception ignored) {
                source = source.getAbsoluteFile();
            }

            if (!scannedSources.add(source)) {
                continue;
            }
            if (source.isDirectory()) {
                scanDirectory(source, locations);
            } else if (source.isFile()) {
                scanArchive(source, locations);
            }
        }

        List<ResourceLocation> sorted = new ArrayList<ResourceLocation>(locations);
        Collections.sort(sorted, new Comparator<ResourceLocation>() {
            @Override
            public int compare(ResourceLocation left, ResourceLocation right) {
                return left.toString().compareTo(right.toString());
            }
        });
        return sorted;
    }

    private static void collectResourcePacks(
            SimpleReloadableResourceManager manager,
            Set<IResourcePack> result
    ) {
        for (Field field : allFields(manager.getClass())) {
            if (!Map.class.isAssignableFrom(field.getType())) {
                continue;
            }
            Object value = readField(field, manager);
            if (!(value instanceof Map)) {
                continue;
            }
            for (Object fallback : ((Map<?, ?>) value).values()) {
                if (fallback != null) {
                    collectPackLists(fallback, result);
                }
            }
        }
    }

    private static void collectPackLists(Object fallback, Set<IResourcePack> result) {
        for (Field field : allFields(fallback.getClass())) {
            if (!List.class.isAssignableFrom(field.getType())) {
                continue;
            }
            Object value = readField(field, fallback);
            if (!(value instanceof List)) {
                continue;
            }
            for (Object candidate : (List<?>) value) {
                if (candidate instanceof IResourcePack) {
                    result.add((IResourcePack) candidate);
                }
            }
        }
    }

    private static File findPackSource(IResourcePack pack) {
        for (Field field : allFields(pack.getClass())) {
            if (File.class.isAssignableFrom(field.getType())) {
                Object value = readField(field, pack);
                if (value instanceof File) {
                    return (File) value;
                }
            }
        }

        // Forge's FML resource-pack wrappers expose a ModContainer instead of a File.
        try {
            Method getContainer = pack.getClass().getMethod("getFMLContainer");
            Object container = getContainer.invoke(pack);
            Method getSource = container.getClass().getMethod("getSource");
            Object source = getSource.invoke(container);
            return source instanceof File ? (File) source : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void scanDirectory(File root, Set<ResourceLocation> result) {
        File assets = new File(root, ResourcePathResolver.ASSETS_DIRECTORY);
        File[] namespaces = assets.listFiles();
        if (namespaces == null) {
            return;
        }
        for (File namespace : namespaces) {
            if (!namespace.isDirectory()) {
                continue;
            }
            File guiModifiers = new File(namespace, PREFIX.replace('/', File.separatorChar));
            collectJsonFiles(root, namespace.getName(), guiModifiers, result);
        }
    }

    private static void collectJsonFiles(
            File root,
            String namespace,
            File current,
            Set<ResourceLocation> result
    ) {
        File[] children = current.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                collectJsonFiles(root, namespace, child, result);
            } else if (child.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".json")) {
                String relative = new File(
                        root,
                        ResourcePathResolver.ASSETS_DIRECTORY + File.separator + namespace
                )
                        .toURI().relativize(child.toURI()).getPath();
                result.add(new ResourceLocation(namespace, relative));
            }
        }
    }

    private static void scanArchive(File archive, Set<ResourceLocation> result) {
        ZipFile zip = null;
        try {
            zip = new ZipFile(archive);
            java.util.Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                addArchiveEntry(entry.getName(), result);
            }
        } catch (Exception error) {
            RedFoxExpand.LOGGER.warn("Could not scan resource pack {}", archive, error);
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static void addArchiveEntry(String name, Set<ResourceLocation> result) {
        String normalized = name.replace('\\', '/');
        if (!normalized.startsWith(ResourcePathResolver.ASSETS_ROOT)
                || !normalized.endsWith(".json")) {
            return;
        }

        int namespaceEnd = normalized.indexOf('/', ResourcePathResolver.ASSETS_ROOT.length());
        if (namespaceEnd < 0) {
            return;
        }
        String namespace = normalized.substring(
                ResourcePathResolver.ASSETS_ROOT.length(),
                namespaceEnd
        );
        String path = normalized.substring(namespaceEnd + 1);
        if (path.startsWith(PREFIX)) {
            result.add(new ResourceLocation(namespace, path));
        }
    }

    private static Object readField(Field field, Object owner) {
        try {
            field.setAccessible(true);
            return field.get(owner);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static List<Field> allFields(Class<?> type) {
        List<Field> result = new ArrayList<Field>();
        for (Class<?> cursor = type; cursor != null; cursor = cursor.getSuperclass()) {
            Collections.addAll(result, cursor.getDeclaredFields());
        }
        return result;
    }
}
