package redfoxexpand.reactive.behavior;

import redfoxexpand.reactive.expression.CompiledExpression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Schema v3 WHEN + IF + DO rule. */
public final class BehaviorRule {
    private final EventTrigger trigger;
    private final CompiledExpression condition;
    private final List<Action> actions;

    public BehaviorRule(EventTrigger trigger, CompiledExpression condition, List<Action> actions) {
        this.trigger = trigger;
        this.condition = condition;
        this.actions = Collections.unmodifiableList(new ArrayList<Action>(actions));
    }

    public EventTrigger getTrigger() { return trigger; }
    public CompiledExpression getCondition() { return condition; }
    public List<Action> getActions() { return actions; }
}
