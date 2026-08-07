package redfoxexpand.client.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import redfoxexpand.client.gui.JsonSupport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Parsed, immutable animation metadata. */
public final class AnimationDefinition {

    public enum MissingFrameBehavior {
        USE_DEFAULT,
        SKIP,
        DISABLE;

        private static MissingFrameBehavior parse(String value) {
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            if ("use_default".equals(normalized) || "default".equals(normalized)) {
                return USE_DEFAULT;
            }
            if ("skip".equals(normalized)) {
                return SKIP;
            }
            if ("disable".equals(normalized)) {
                return DISABLE;
            }
            throw new IllegalArgumentException("Unknown missing_frame behavior: " + value);
        }
    }

    public static final class Frame {
        public final String texture;
        public final int durationMillis;

        private Frame(String texture, int durationMillis) {
            this.texture = texture;
            this.durationMillis = durationMillis;
        }
    }

    public final List<Frame> frames;
    public final boolean loop;
    public final String playbackCondition;
    public final String defaultTexture;
    public final MissingFrameBehavior missingFrameBehavior;

    private AnimationDefinition(
            List<Frame> frames,
            boolean loop,
            String playbackCondition,
            String defaultTexture,
            MissingFrameBehavior missingFrameBehavior
    ) {
        this.frames = Collections.unmodifiableList(frames);
        this.loop = loop;
        this.playbackCondition = playbackCondition;
        this.defaultTexture = defaultTexture;
        this.missingFrameBehavior = missingFrameBehavior;
    }

    public static AnimationDefinition parse(JsonObject json) {
        int defaultDuration = positiveDuration(
                JsonSupport.integer(json, "frame_duration_ms", 100),
                "frame_duration_ms"
        );
        JsonElement frameElement = json.get("frames");
        if (frameElement == null || !frameElement.isJsonArray()) {
            throw new IllegalArgumentException("animation frames must be an array");
        }

        JsonArray frameArray = frameElement.getAsJsonArray();
        if (frameArray.size() == 0 || frameArray.size() > 4096) {
            throw new IllegalArgumentException("animation must contain 1..4096 frames");
        }
        List<Frame> frames = new ArrayList<Frame>();
        for (JsonElement element : frameArray) {
            String texture;
            int duration;
            if (element.isJsonPrimitive()) {
                texture = element.getAsString().trim();
                duration = defaultDuration;
            } else if (element.isJsonObject()) {
                JsonObject frame = element.getAsJsonObject();
                texture = JsonSupport.string(frame, "texture", "").trim();
                duration = positiveDuration(
                        JsonSupport.integer(frame, "duration_ms", defaultDuration),
                        "frame duration_ms"
                );
            } else {
                throw new IllegalArgumentException("animation frame must be a path or object");
            }
            if (texture.isEmpty()) {
                throw new IllegalArgumentException("animation frame texture must not be empty");
            }
            frames.add(new Frame(texture, duration));
        }

        String condition = JsonSupport.string(json, "condition", "always")
                .trim()
                .toLowerCase(Locale.ROOT);
        if (!"always".equals(condition) && !"never".equals(condition)) {
            throw new IllegalArgumentException(
                    "Unsupported animation condition (currently always or never): " + condition
            );
        }

        return new AnimationDefinition(
                frames,
                JsonSupport.bool(json, "loop", true),
                condition,
                JsonSupport.string(json, "default_texture", "").trim(),
                MissingFrameBehavior.parse(
                        JsonSupport.string(json, "missing_frame", "use_default")
                )
        );
    }

    private static int positiveDuration(int value, String field) {
        if (value <= 0 || value > 600000) {
            throw new IllegalArgumentException(field + " must be between 1 and 600000");
        }
        return value;
    }
}
