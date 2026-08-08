package redfoxexpand.client.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/** Applies stable definition IDs and explicit append/replace/disable operations. */
public final class DefinitionRegistry {

    private DefinitionRegistry() {
    }

    public static List<GuiDefinition> resolve(List<GuiDefinition> candidates) {
        List<GuiDefinition> active = new ArrayList<GuiDefinition>();
        for (GuiDefinition candidate : candidates) {
            if (candidate.operation != GuiDefinition.Operation.APPEND) {
                removeId(active, candidate.id);
            }
            if (candidate.operation != GuiDefinition.Operation.DISABLE) {
                active.add(candidate);
            }
        }
        Collections.sort(active, new Comparator<GuiDefinition>() {
            @Override
            public int compare(GuiDefinition left, GuiDefinition right) {
                return Integer.compare(left.priority, right.priority);
            }
        });
        return Collections.unmodifiableList(active);
    }

    private static void removeId(List<GuiDefinition> active, String id) {
        Iterator<GuiDefinition> iterator = active.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().id.equals(id)) {
                iterator.remove();
            }
        }
    }
}
