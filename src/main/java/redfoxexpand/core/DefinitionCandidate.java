package redfoxexpand.core;

/** One source-aware definition before Registry operations are applied. */
public record DefinitionCandidate(
        String id,
        int apiVersion,
        String sourcePack,
        int sourcePriority,
        String sourcePath,
        int sourceIndex,
        MatchSpec matcher,
        Operation operation,
        int priority,
        GuiDefinition definition
) {
    public enum Operation { APPEND, REPLACE, DISABLE }
}
