package redfoxexpand.core;

/** One source-aware definition before Registry operations are applied. */
public final class DefinitionCandidate {
    public enum Operation { APPEND, REPLACE, DISABLE }

    private final String id;
    private final int apiVersion;
    private final String sourcePack;
    private final int sourcePriority;
    private final String sourcePath;
    private final int sourceIndex;
    private final MatchSpec matcher;
    private final Operation operation;
    private final int priority;
    private final GuiDefinition definition;

    public DefinitionCandidate(String id, int apiVersion, String sourcePack, int sourcePriority,
                               String sourcePath, int sourceIndex, MatchSpec matcher,
                               Operation operation, int priority, GuiDefinition definition) {
        this.id = id;
        this.apiVersion = apiVersion;
        this.sourcePack = sourcePack;
        this.sourcePriority = sourcePriority;
        this.sourcePath = sourcePath;
        this.sourceIndex = sourceIndex;
        this.matcher = matcher;
        this.operation = operation;
        this.priority = priority;
        this.definition = definition;
    }

    public String id() { return id; }
    public int apiVersion() { return apiVersion; }
    public String sourcePack() { return sourcePack; }
    public int sourcePriority() { return sourcePriority; }
    public String sourcePath() { return sourcePath; }
    public int sourceIndex() { return sourceIndex; }
    public MatchSpec matcher() { return matcher; }
    public Operation operation() { return operation; }
    public int priority() { return priority; }
    public GuiDefinition definition() { return definition; }
}

