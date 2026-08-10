package redfoxexpand.reactive.behavior;

import redfoxexpand.reactive.event.RuntimeEvent;
import redfoxexpand.reactive.runtime.RuntimeContext;

public interface ActionSink {
    void execute(Action action, RuntimeEvent event, RuntimeContext context);
}
