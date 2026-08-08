package redfoxexpand.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/** Deterministic append/replace/disable merge over pack-aware candidates. */
public final class DefinitionRegistry {
    private static final Comparator<DefinitionCandidate> ORDER = Comparator
            .comparingInt(DefinitionCandidate::sourcePriority)
            .thenComparingInt(DefinitionCandidate::priority)
            .thenComparing(DefinitionCandidate::sourcePath)
            .thenComparingInt(DefinitionCandidate::sourceIndex);

    private DefinitionRegistry() {
    }

    public static List<DefinitionCandidate> resolve(List<DefinitionCandidate> input) {
        List<DefinitionCandidate> ordered = new ArrayList<>(input);
        ordered.sort(ORDER);
        List<DefinitionCandidate> active = new ArrayList<>();
        for (DefinitionCandidate candidate : ordered) {
            if (candidate.operation() != DefinitionCandidate.Operation.APPEND) {
                removeId(active, candidate.id());
            }
            if (candidate.operation() != DefinitionCandidate.Operation.DISABLE) {
                active.add(candidate);
            }
        }
        return List.copyOf(active);
    }

    private static void removeId(List<DefinitionCandidate> active, String id) {
        Iterator<DefinitionCandidate> iterator = active.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().id().equals(id)) iterator.remove();
        }
    }
}
