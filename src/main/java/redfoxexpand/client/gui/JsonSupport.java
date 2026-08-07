package redfoxexpand.client.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Locale;

public final class JsonSupport {

    private JsonSupport() {
    }

    public static String string(JsonObject json, String key, String fallback) {
        JsonElement value = json.get(key);
        return value == null || value.isJsonNull() ? fallback : value.getAsString();
    }

    public static int integer(JsonObject json, String key, int fallback) {
        JsonElement value = json.get(key);
        return value == null || value.isJsonNull() ? fallback : value.getAsInt();
    }

    public static int integerAlias(JsonObject json, String primary, String alias, int fallback) {
        return json.has(primary) ? integer(json, primary, fallback) : integer(json, alias, fallback);
    }

    public static float floating(JsonObject json, String key, float fallback) {
        JsonElement value = json.get(key);
        return value == null || value.isJsonNull() ? fallback : value.getAsFloat();
    }

    public static float floatingAlias(JsonObject json, String primary, String alias, float fallback) {
        return json.has(primary) ? floating(json, primary, fallback) : floating(json, alias, fallback);
    }

    public static boolean bool(JsonObject json, String key, boolean fallback) {
        JsonElement value = json.get(key);
        return value == null || value.isJsonNull() ? fallback : value.getAsBoolean();
    }

    public static Integer color(JsonObject json, String key) {
        JsonElement value = json.get(key);
        if (value == null || value.isJsonNull()) {
            return null;
        }
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
            return value.getAsInt();
        }

        String raw = value.getAsString().trim().replace("_", "");
        boolean negative = raw.startsWith("-");
        if (negative) {
            return Integer.parseInt(raw);
        }
        if (raw.startsWith("#")) {
            raw = raw.substring(1);
        } else if (raw.toLowerCase(Locale.ROOT).startsWith("0x")) {
            raw = raw.substring(2);
        }
        if (raw.length() != 6 && raw.length() != 8) {
            throw new IllegalArgumentException("Color must be #RRGGBB or #AARRGGBB: " + value);
        }
        return (int) Long.parseLong(raw, 16);
    }
}
