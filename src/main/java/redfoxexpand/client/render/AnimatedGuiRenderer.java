package redfoxexpand.client.render;

import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Time-based frame selector for an animation whose JSON and PNG files have
 * already been resolved and cached by the reload handler.
 */
public final class AnimatedGuiRenderer implements GuiTexture {

    public static final class Frame {
        public final ResourceLocation texture;
        public final int durationMillis;

        public Frame(ResourceLocation texture, int durationMillis) {
            if (texture == null) {
                throw new IllegalArgumentException("frame texture must not be null");
            }
            if (durationMillis <= 0) {
                throw new IllegalArgumentException("frame duration must be positive");
            }
            this.texture = texture;
            this.durationMillis = durationMillis;
        }
    }

    private final List<Frame> frames;
    private final ResourceLocation defaultTexture;
    private final boolean loop;
    private final AnimationPlaybackCondition condition;
    private final long startedAtMillis;
    private final long totalDurationMillis;

    public AnimatedGuiRenderer(
            List<Frame> frames,
            ResourceLocation defaultTexture,
            boolean loop,
            AnimationPlaybackCondition condition,
            long startedAtMillis
    ) {
        if (frames == null || frames.isEmpty()) {
            throw new IllegalArgumentException("animation must contain at least one cached frame");
        }
        if (defaultTexture == null) {
            throw new IllegalArgumentException("animation default texture must not be null");
        }
        if (condition == null) {
            throw new IllegalArgumentException("animation condition must not be null");
        }
        this.frames = Collections.unmodifiableList(new ArrayList<Frame>(frames));
        this.defaultTexture = defaultTexture;
        this.loop = loop;
        this.condition = condition;
        this.startedAtMillis = startedAtMillis;

        long total = 0L;
        for (Frame frame : frames) {
            total += frame.durationMillis;
        }
        this.totalDurationMillis = total;
    }

    @Override
    public ResourceLocation textureAt(long nowMillis) {
        if (!condition.shouldPlay()) {
            return defaultTexture;
        }

        long elapsed = Math.max(0L, nowMillis - startedAtMillis);
        if (!loop && elapsed >= totalDurationMillis) {
            return defaultTexture;
        }
        long position = loop ? elapsed % totalDurationMillis : elapsed;
        for (Frame frame : frames) {
            if (position < frame.durationMillis) {
                return frame.texture;
            }
            position -= frame.durationMillis;
        }
        return defaultTexture;
    }

    @Override
    public boolean isAnimated() {
        return true;
    }
}
