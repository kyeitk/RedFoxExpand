package redfoxexpand.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/** Deterministic append/replace/disable merge over pack-aware candidates. */
public final class DefinitionRegistry {
    private static final Comparator<DefinitionCandidate> ORDER = new Comparator<DefinitionCandidate>() {
        @Override
        public int compare(DefinitionCandidate left, DefinitionCandidate right) {
            int result = Integer.compare(left.sourcePriority(), right.sourcePriority());
            if (result != 0) return result;
            result = Integer.compare(left.priority(), right.priority());
            if (result != 0) return result;
            result = left.sourcePath().compareTo(right.sourcePath());
            return result != 0 ? result : Integer.compare(left.sourceIndex(), right.sourceIndex());
        }
    };

    private DefinitionRegistry() { }

    public static List<DefinitionCandidate> resolve(List<DefinitionCandidate> input) {
        List<DefinitionCandidate> ordered = new ArrayList<DefinitionCandidate>(input);
        Collections.sort(ordered, ORDER);
        List<DefinitionCandidate> active = new ArrayList<DefinitionCandidate>();
        for (DefinitionCandidate candidate : ordered) {
            if (candidate.operation() != DefinitionCandidate.Operation.APPEND) {
                removeId(active, candidate.id());
            }
            if (candidate.operation() != DefinitionCandidate.Operation.DISABLE) active.add(candidate);
        }
        return Collections.unmodifiableList(active);
    }

    private static void removeId(List<DefinitionCandidate> active, String id) {
        Iterator<DefinitionCandidate> iterator = active.iterator();
        while (iterator.hasNext()) if (iterator.next().id().equals(id)) iterator.remove();
    }
}

