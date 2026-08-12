package redfoxexpand.client.gui;

import net.minecraft.world.inventory.Slot;
import redfoxexpand.core.GuiContext;
import redfoxexpand.core.GuiDefinition;
import redfoxexpand.core.ResolvedModifier;
import redfoxexpand.core.ResourceLimits;
import redfoxexpand.mixin.client.SlotAccessor;

import java.util.ArrayList;
import java.util.List;

/** Cooperative Slot transform: subtract only RedFoxExpand's last delta, then reapply. */
final class SlotTransformController {
    private SlotTransformController() {
    }

    static void restore(GuiRuntimeState state) {
        for (var entry : new ArrayList<>(state.slotDeltas.entrySet())) {
            Slot slot = entry.getKey();
            GuiRuntimeState.Delta delta = entry.getValue();
            SlotAccessor access = (SlotAccessor) slot;
            access.redfoxexpand$setX(slot.x - delta.x());
            access.redfoxexpand$setY(slot.y - delta.y());
        }
        state.slotDeltas.clear();
    }

    static void apply(GuiRuntimeState state, ResolvedModifier modifier, GuiContext context) {
        List<Slot> actual = state.screen.getMenu().slots;
        List<GuiContext.SlotContext> bases = context.slots();
        int count = Math.min(actual.size(), bases.size());
        for (int index = 0; index < count; index++) {
            Slot slot = actual.get(index);
            GuiContext.SlotContext base = bases.get(index);
            int dx = 0;
            int dy = 0;
            for (GuiDefinition.SlotRule rule : modifier.slotRules()) {
                if (rule.matches(base)) {
                    dx = ResourceLimits.boundedInt((double) dx + rule.xOffset(), "slot x offset");
                    dy = ResourceLimits.boundedInt((double) dy + rule.yOffset(), "slot y offset");
                }
            }
            if (dx != 0 || dy != 0) {
                SlotAccessor access = (SlotAccessor) slot;
                access.redfoxexpand$setX(slot.x + dx);
                access.redfoxexpand$setY(slot.y + dy);
                state.slotDeltas.put(slot, new GuiRuntimeState.Delta(dx, dy));
            }
        }
    }
}

