package redfoxexpand.reactive.event;

import redfoxexpand.reactive.runtime.RuntimeSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Derives one-shot events from adjacent client-tick snapshots. */
public final class EventDetector {
    public List<RuntimeEvent> detect(RuntimeSnapshot previous, RuntimeSnapshot current) {
        if (previous == null || current == null) return Collections.emptyList();
        List<RuntimeEvent> events = new ArrayList<RuntimeEvent>(2);
        double oldHealth = previous.number("player.health");
        double newHealth = current.number("player.health");
        if (newHealth < oldHealth) {
            events.add(RuntimeEvent.health(RuntimeEvent.HEALTH_DECREASED, oldHealth, newHealth));
        } else if (newHealth > oldHealth) {
            events.add(RuntimeEvent.health(RuntimeEvent.HEALTH_INCREASED, oldHealth, newHealth));
        }
        boolean oldBurning = previous.bool("player.is_burning");
        boolean newBurning = current.bool("player.is_burning");
        if (!oldBurning && newBurning) events.add(RuntimeEvent.state(RuntimeEvent.STARTED_BURNING));
        else if (oldBurning && !newBurning) events.add(RuntimeEvent.state(RuntimeEvent.STOPPED_BURNING));
        return Collections.unmodifiableList(events);
    }
}

