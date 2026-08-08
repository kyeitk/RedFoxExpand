package redfoxexpand.client.gui;

import com.google.gson.JsonObject;
import net.minecraft.client.resources.I18n;

/** Explicit selector for one foreground font draw. */
public final class FontRule {

    private final String text;
    private final String translationKey;
    private final Integer matchX;
    private final Integer matchY;
    private final Integer ordinal;
    public final int xOffset;
    public final int yOffset;
    public final Integer color;

    private FontRule(
            String text,
            String translationKey,
            Integer matchX,
            Integer matchY,
            Integer ordinal,
            int xOffset,
            int yOffset,
            Integer color
    ) {
        this.text = text;
        this.translationKey = translationKey;
        this.matchX = matchX;
        this.matchY = matchY;
        this.ordinal = ordinal;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.color = color;
    }

    public static FontRule parse(JsonObject json) {
        String text = JsonSupport.string(json, "text", null);
        String translationKey = JsonSupport.string(json, "translation_key", null);
        Integer matchX = json.has("match_x")
                ? JsonSupport.integer(json, "match_x", 0)
                : null;
        Integer matchY = json.has("match_y")
                ? JsonSupport.integer(json, "match_y", 0)
                : null;
        Integer ordinal = json.has("ordinal")
                ? JsonSupport.integer(json, "ordinal", 0)
                : null;
        if (text == null && translationKey == null
                && matchX == null && matchY == null && ordinal == null) {
            throw new IllegalArgumentException("font_rule requires an explicit selector");
        }
        if (ordinal != null && (ordinal.intValue() < 0 || ordinal.intValue() > 1024)) {
            throw new IllegalArgumentException("font_rule ordinal must be 0..1024");
        }
        return new FontRule(
                text,
                translationKey,
                matchX,
                matchY,
                ordinal,
                JsonSupport.integer(json, "x_offset", 0),
                JsonSupport.integer(json, "y_offset", 0),
                JsonSupport.color(json, "color")
        );
    }

    public boolean matches(String actualText, int x, int y, int callOrdinal) {
        if (text != null && !text.equals(actualText)) {
            return false;
        }
        if (translationKey != null && !I18n.format(translationKey).equals(actualText)) {
            return false;
        }
        if (matchX != null && matchX.intValue() != x) {
            return false;
        }
        if (matchY != null && matchY.intValue() != y) {
            return false;
        }
        return ordinal == null || ordinal.intValue() == callOrdinal;
    }
}
