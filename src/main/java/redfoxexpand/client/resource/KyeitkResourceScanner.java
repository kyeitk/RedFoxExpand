package redfoxexpand.client.resource;

import redfoxexpand.RedFoxExpand;
import redfoxexpand.client.compat.ModResourceAdapter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.client.resources.SimpleReloadableResourceManager;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Direct scanner for the physical assets/Kyeitk directory. Minecraft 1.7.10
 * ignores uppercase resource domains, so these resources cannot be obtained
 * through IResourceManager and must be read from active pack sources.
 */
public final class KyeitkResourceScanner {

    private KyeitkResourceScanner() {
    }

    public static ResourceIndex scan(SimpleReloadableResourceManager manager) {
        List<IResourcePack> packs = activePacks(manager);
        List<File> sources = new ArrayList<File>();
        Set<File> seen = new LinkedHashSet<File>();
        for (IResourcePack pack : packs) {
            File source = findPackSource(pack);
            if (source == null) {
                continue;
            }
            source = canonical(source);
            if (!seen.add(source)) {
                sources.remove(source);
            }
            sources.add(source);
        }
        return scanSources(sources);
    }

    static ResourceIndex scanSources(List<File> sources) {
        Map<String, ResourceFile> winning = new LinkedHashMap<String, ResourceFile>();
        for (File source : sources) {
            if (source.isDirectory()) {
                scanDirectory(source, winning);
            } else if (source.isFile()) {
                scanArchive(source, winning);
            }
        }
        return new ResourceIndex(winning);
    }

    private static void scanDirectory(File source, Map<String, ResourceFile> winning) {
        File assets = new File(source, ResourcePathResolver.ASSETS_DIRECTORY);
        if (!assets.isDirectory()) {
            return;
        }

        List<File> namespaceDirectories = new ArrayList<File>();
        File lower = new File(assets, ResourcePathResolver.LOWERCASE_NAMESPACE);
        File canonical = new File(assets, ResourcePathResolver.PHYSICAL_NAMESPACE);
        if (lower.isDirectory()) {
            namespaceDirectories.add(lower);
        }
        if (canonical.isDirectory() && !sameFile(lower, canonical)) {
            namespaceDirectories.add(canonical);
        }
        for (File namespace : namespaceDirectories) {
            collectDirectoryFiles(
                    canonical(source),
                    canonical(namespace),
                    canonical(namespace),
                    winning,
                    new LinkedHashSet<File>()
            );
        }
    }

    private static void collectDirectoryFiles(
            File packRoot,
            File namespaceRoot,
            File current,
            Map<String, ResourceFile> winning,
            Set<File> visitedDirectories
    ) {
        current = canonical(current);
        if (!isWithin(namespaceRoot, current) || !visitedDirectories.add(current)) {
            return;
        }
        File[] children = current.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                collectDirectoryFiles(
                        packRoot,
                        namespaceRoot,
                        child,
                        winning,
                        visitedDirectories
                );
            } else if (child.isFile()) {
                child = canonical(child);
                if (!isWithin(namespaceRoot, child)) {
                    continue;
                }
                String relative = namespaceRoot.toURI().relativize(child.toURI()).getPath();
                String normalized = ResourcePathResolver.normalizeRelativePath(relative);
                if (normalized.length() > ResourceLimits.MAX_RESOURCE_PATH_LENGTH
                        || winning.size() >= ResourceLimits.MAX_DISCOVERED_RESOURCES) {
                    RedFoxExpand.LOGGER.warn("Kyeitk directory resource budget reached in {}", packRoot);
                    return;
                }
                String entryName = packRoot.toURI().relativize(child.toURI()).getPath();
                winning.put(normalized, new ResourceFile(packRoot, entryName, normalized));
            }
        }
    }

    private static void scanArchive(File source, Map<String, ResourceFile> winning) {
        ZipFile zip = null;
        try {
            zip = new ZipFile(source);
            List<String> lowercaseEntries = new ArrayList<String>();
            List<String> canonicalEntries = new ArrayList<String>();
            java.util.Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName().replace('\\', '/');
                if (name.startsWith(ResourcePathResolver.ASSETS_ROOT
                        + ResourcePathResolver.LOWERCASE_NAMESPACE + "/")) {
                    lowercaseEntries.add(name);
                } else if (name.startsWith(ResourcePathResolver.ASSETS_ROOT
                        + ResourcePathResolver.PHYSICAL_NAMESPACE + "/")) {
                    canonicalEntries.add(name);
                }
            }
            addArchiveEntries(source, lowercaseEntries, winning);
            addArchiveEntries(source, canonicalEntries, winning);
        } catch (Exception error) {
            RedFoxExpand.LOGGER.warn("Could not scan Kyeitk resources in {}", source, error);
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static void addArchiveEntries(
            File source,
            List<String> entries,
            Map<String, ResourceFile> winning
    ) {
        Collections.sort(entries);
        for (String entry : entries) {
            try {
                int namespaceEnd = entry.indexOf('/', ResourcePathResolver.ASSETS_ROOT.length());
                String relative = ResourcePathResolver.normalizeRelativePath(
                        entry.substring(namespaceEnd + 1)
                );
                if (relative.length() > ResourceLimits.MAX_RESOURCE_PATH_LENGTH
                        || winning.size() >= ResourceLimits.MAX_DISCOVERED_RESOURCES) {
                    RedFoxExpand.LOGGER.warn("Kyeitk archive resource budget reached in {}", source);
                    return;
                }
                winning.put(relative, new ResourceFile(source, entry, relative));
            } catch (IllegalArgumentException error) {
                RedFoxExpand.LOGGER.warn("Ignoring unsafe Kyeitk ZIP entry {} in {}", entry, source);
            }
        }
    }

    static List<IResourcePack> activePacks(SimpleReloadableResourceManager manager) {
        List<IResourcePack> discovered = new ArrayList<IResourcePack>();
        collectMinecraftPackLists(discovered);
        collectManagerPacks(manager, discovered);

        List<IResourcePack> selected = new ArrayList<IResourcePack>();
        IResourcePack serverPack = null;
        List<IResourcePack> repositoryPacks = new ArrayList<IResourcePack>();
        try {
            ResourcePackRepository repository = Minecraft.getMinecraft().getResourcePackRepository();
            for (Object candidate : repository.getRepositoryEntries()) {
                ResourcePackRepository.Entry entry = (ResourcePackRepository.Entry) candidate;
                repositoryPacks.add(entry.getResourcePack());
            }
            serverPack = repository.getResourcePackInstance();
        } catch (Throwable error) {
            RedFoxExpand.LOGGER.debug("Could not query the resource-pack repository", error);
        }

        for (IResourcePack pack : discovered) {
            if (!containsIdentity(repositoryPacks, pack) && pack != serverPack) {
                addIdentity(selected, pack);
            }
        }
        for (IResourcePack pack : repositoryPacks) {
            addIdentity(selected, pack);
        }
        if (serverPack != null) {
            addIdentity(selected, serverPack);
        }
        return selected;
    }

    private static void collectMinecraftPackLists(List<IResourcePack> result) {
        try {
            Object minecraft = Minecraft.getMinecraft();
            for (Field field : allFields(minecraft.getClass())) {
                if (!List.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                Object value = readField(field, minecraft);
                if (!(value instanceof List)) {
                    continue;
                }
                for (Object candidate : (List<?>) value) {
                    if (candidate instanceof IResourcePack) {
                        addIdentity(result, (IResourcePack) candidate);
                    }
                }
            }
        } catch (Throwable error) {
            RedFoxExpand.LOGGER.debug("Could not query Minecraft default resource packs", error);
        }
    }

    private static void collectManagerPacks(
            SimpleReloadableResourceManager manager,
            List<IResourcePack> result
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
                if (fallback == null) {
                    continue;
                }
                for (Field fallbackField : allFields(fallback.getClass())) {
                    if (!List.class.isAssignableFrom(fallbackField.getType())) {
                        continue;
                    }
                    Object list = readField(fallbackField, fallback);
                    if (!(list instanceof List)) {
                        continue;
                    }
                    for (Object candidate : (List<?>) list) {
                        if (candidate instanceof IResourcePack) {
                            addIdentity(result, (IResourcePack) candidate);
                        }
                    }
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

    private static Object readField(Field field, Object owner) {
        try {
            field.setAccessible(true);
            return field.get(owner);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static List<Field> allFields(Class<?> type) {
        List<Field> fields = new ArrayList<Field>();
        for (Class<?> cursor = type; cursor != null; cursor = cursor.getSuperclass()) {
            Collections.addAll(fields, cursor.getDeclaredFields());
        }
        return fields;
    }

    private static void addIdentity(List<IResourcePack> result, IResourcePack pack) {
        if (!containsIdentity(result, pack)) {
            result.add(pack);
        }
    }

    private static boolean containsIdentity(List<IResourcePack> packs, IResourcePack target) {
        for (IResourcePack pack : packs) {
            if (pack == target) {
                return true;
            }
        }
        return false;
    }

    private static File canonical(File file) {
        try {
            return file.getCanonicalFile();
        } catch (IOException ignored) {
            return file.getAbsoluteFile();
        }
    }

    private static boolean sameFile(File left, File right) {
        return left.isDirectory() && right.isDirectory() && canonical(left).equals(canonical(right));
    }

    private static boolean isWithin(File root, File candidate) {
        return candidate.toPath().normalize().startsWith(root.toPath().normalize());
    }

    public static final class ResourceIndex {
        private final Map<String, ResourceFile> resources;

        private ResourceIndex(Map<String, ResourceFile> resources) {
            this.resources = Collections.unmodifiableMap(
                    new LinkedHashMap<String, ResourceFile>(resources)
            );
        }

        public ResourceFile get(String path) {
            return resources.get(ResourcePathResolver.normalizeRelativePath(path));
        }

        public ResourceFile require(String path) {
            ResourceFile resource = get(path);
            if (resource == null) {
                throw new IllegalArgumentException("Missing Kyeitk resource: " + path);
            }
            return resource;
        }

        public List<ResourceFile> guiConfigs() {
            List<ResourceFile> configs = new ArrayList<ResourceFile>();
            for (ResourceFile resource : resources.values()) {
                if (resource.path.endsWith(".json")
                        && (resource.path.startsWith(ResourcePathResolver.CONFIG_ROOT)
                        || ModResourceAdapter.isApplicableConfig(resource.path))) {
                    configs.add(resource);
                }
            }
            Collections.sort(configs, new Comparator<ResourceFile>() {
                @Override
                public int compare(ResourceFile left, ResourceFile right) {
                    return left.path.compareTo(right.path);
                }
            });
            return configs;
        }

        public int size() {
            return resources.size();
        }
    }

    public static final class ResourceFile {
        private final File source;
        private final String entryName;
        public final String path;

        private ResourceFile(File source, String entryName, String path) {
            this.source = source;
            this.entryName = entryName;
            this.path = path;
        }

        public InputStream open() throws IOException {
            if (source.isDirectory()) {
                return new BufferedInputStream(new FileInputStream(new File(source, entryName)));
            }
            final ZipFile zip = new ZipFile(source);
            ZipEntry entry = zip.getEntry(entryName);
            if (entry == null) {
                zip.close();
                throw new IOException("Missing ZIP entry " + entryName + " in " + source);
            }
            return new FilterInputStream(zip.getInputStream(entry)) {
                @Override
                public void close() throws IOException {
                    try {
                        super.close();
                    } finally {
                        zip.close();
                    }
                }
            };
        }

        @Override
        public String toString() {
            return ResourcePathResolver.PHYSICAL_NAMESPACE + ":" + path;
        }
    }
}
