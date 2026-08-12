package redfoxexpand.client.gui;

import redfoxexpand.core.GuiDefinition;
import redfoxexpand.reactive.property.FinalRenderProperties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable root-to-leaf Schema v3.1 transform chain for one rendered Sprite. */
final class SceneRenderState {
    private final List<Node> nodes;

    SceneRenderState(List<Node> nodes) {
        this.nodes = Collections.unmodifiableList(new ArrayList<Node>(nodes));
    }

    List<Node> nodes() { return nodes; }

    static final class Node {
        final GuiDefinition.Anchor anchor;
        final double x;
        final double y;
        final GuiDefinition.Pivot pivot;
        final FinalRenderProperties properties;

        Node(GuiDefinition.Anchor anchor, double x, double y, GuiDefinition.Pivot pivot,
             FinalRenderProperties properties) {
            this.anchor = anchor;
            this.x = x;
            this.y = y;
            this.pivot = pivot;
            this.properties = properties;
        }
    }
}
