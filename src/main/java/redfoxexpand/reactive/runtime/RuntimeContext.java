package redfoxexpand.reactive.runtime;

import redfoxexpand.reactive.value.RuntimeValue;

/** Platform-neutral variable lookup used by compiled expressions. */
public interface RuntimeContext {
    RuntimeValue get(String name);
}
