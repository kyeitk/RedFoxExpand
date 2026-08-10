package redfoxexpand.reactive.behavior;

import redfoxexpand.reactive.event.RuntimeEvent;
import redfoxexpand.reactive.runtime.RuntimeContext;
import redfoxexpand.reactive.runtime.RuntimeDiagnostics;

import java.util.ArrayList;
import java.util.List;

/** Stateful per-screen behavior evaluator; events are processed once per client tick. */
public final class BehaviorEngine {
    private final List<RuleState> rules;
    private final RuntimeDiagnostics diagnostics;

    public BehaviorEngine(List<BehaviorRule> definitions, RuntimeDiagnostics diagnostics) {
        this.rules = new ArrayList<RuleState>(definitions.size());
        for (BehaviorRule definition : definitions) rules.add(new RuleState(definition));
        this.diagnostics = diagnostics;
    }

    public void process(List<RuntimeEvent> events, RuntimeContext state, ActionSink sink) {
        for (RuntimeEvent event : events) {
            for (int index = 0; index < rules.size(); index++) {
                RuleState rule = rules.get(index);
                if (!rule.definition.getTrigger().getEvent().equals(event.getId())) continue;
                if (!passesEvery(rule, event)) continue;
                RuntimeContext context = event.context(state);
                try {
                    if (!rule.definition.getCondition().evaluate(context).asBoolean()) continue;
                    for (Action action : rule.definition.getActions()) sink.execute(action, event, context);
                } catch (RuntimeException error) {
                    if (diagnostics != null) {
                        diagnostics.warning("behavior-" + index, "Schema v3 behavior runtime failure", error);
                    }
                }
            }
        }
    }

    public double accumulator(int ruleIndex) {
        return rules.get(ruleIndex).accumulator;
    }

    private static boolean passesEvery(RuleState rule, RuntimeEvent event) {
        Double every = rule.definition.getTrigger().getEvery();
        if (every == null) return true;
        rule.accumulator += event.getDelta();
        if (rule.accumulator < every.doubleValue()) return false;
        rule.accumulator = rule.accumulator % every.doubleValue();
        return true;
    }

    private static final class RuleState {
        final BehaviorRule definition;
        double accumulator;

        RuleState(BehaviorRule definition) {
            this.definition = definition;
        }
    }
}
