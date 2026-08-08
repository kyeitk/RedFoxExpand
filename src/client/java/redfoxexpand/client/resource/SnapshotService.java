package redfoxexpand.client.resource;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/** Single atomic owner of the currently visible reload generation. */
public final class SnapshotService {
    private static final ResourceSnapshot EMPTY = new ResourceSnapshot(0, List.of(), Map.of(),
            new ResourceSnapshot.LoadReport(0, 0, 0, 0, 0, 0, 0, List.of()));
    private static final AtomicReference<ResourceSnapshot> CURRENT = new AtomicReference<>(EMPTY);

    private SnapshotService() {
    }

    public static ResourceSnapshot current() {
        return CURRENT.get();
    }

    public static void install(ResourceSnapshot snapshot) {
        ResourceSnapshot previous = CURRENT.getAndSet(snapshot);
        if (previous != EMPTY) previous.close();
    }

}
