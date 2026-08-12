package redfoxexpand.client.gui;

import redfoxexpand.core.GuiDefinition;
import redfoxexpand.reactive.property.FinalRenderProperties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable root-to-leaf Schema v3.1 transform chain for one rendered Sprite. */
public final class SceneRenderState {
    private final List<Node> nodes;

    public SceneRenderState(List<Node> nodes) {
        this.nodes = Collections.unmodifiableList(new ArrayList<Node>(nodes));
    }

    public List<Node> nodes() { return nodes; }

    public static final class Node {
        public final GuiDefinition.Anchor anchor;
        public final double x;
        public final double y;
        public final GuiDefinition.Pivot pivot;
        public final FinalRenderProperties properties;

        public Node(GuiDefinition.Anchor anchor, double x, double y, GuiDefinition.Pivot pivot,
             FinalRenderProperties properties) {
            this.anchor = anchor;
            this.x = x;
            this.y = y;
            this.pivot = pivot;
            this.properties = properties;
        }
    }
}

