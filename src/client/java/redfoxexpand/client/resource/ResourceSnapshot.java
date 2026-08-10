package redfoxexpand.client.resource;

import net.minecraft.resources.Identifier;
import redfoxexpand.core.DefinitionCandidate;
import redfoxexpand.core.GuiDefinition;

import java.util.List;
import java.util.Map;

/** Immutable resource generation installed atomically after a successful reload. */
public final class ResourceSnapshot implements AutoCloseable {
    private final long generation;
    private final long startedAtMillis;
    private final List<DefinitionCandidate> definitions;
    private final Map<GuiDefinition.TextureSpec, Identifier> textures;
    private final LoadReport report;

    public ResourceSnapshot(long generation, List<DefinitionCandidate> definitions,
                            Map<GuiDefinition.TextureSpec, Identifier> textures, LoadReport report) {
        this.generation = generation;
        this.startedAtMillis = System.nanoTime() / 1_000_000L;
        this.definitions = List.copyOf(definitions);
        this.textures = Map.copyOf(textures);
        this.report = report;
    }

    public long generation() { return generation; }
    public long startedAtMillis() { return startedAtMillis; }
    public List<DefinitionCandidate> definitions() { return definitions; }
    public Identifier texture(GuiDefinition.TextureSpec spec) { return textures.get(spec); }
    public LoadReport report() { return report; }

    @Override
    public void close() {
        // 26.2 uses native ResourceManager textures; this generation owns no GPU handles.
        // Clearing happens by dropping the immutable snapshot after the atomic swap.
    }

    public record LoadReport(
            int manifests,
            int configs,
            int candidates,
            int activeDefinitions,
            int validatedTextures,
            int warnings,
            int errors,
            List<String> messages
    ) {
        public LoadReport { messages = List.copyOf(messages); }
    }
}
