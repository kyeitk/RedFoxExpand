package redfoxexpand.core;

/** Versioned safety budgets for untrusted resource-pack input. */
public final class ResourceLimits {
    public static final int MAX_JSON_BYTES = 1024 * 1024;
    public static final int MAX_PNG_BYTES = 32 * 1024 * 1024;
    public static final int MAX_MANIFEST_CONFIGS = 256;
    public static final int MAX_CONFIGS_PER_PACK = 256;
    public static final int MAX_DEFINITIONS_PER_FILE = 256;
    public static final int MAX_SPRITES_PER_DEFINITION = 256;
    public static final int MAX_ELEMENTS_PER_DEFINITION = 256;
    public static final int MAX_GROUPS_PER_DEFINITION = 128;
    public static final int MAX_SCENE_DEPTH = 32;
    public static final int MAX_CHILDREN_PER_GROUP = 256;
    public static final int MAX_SLOT_RULES_PER_DEFINITION = 256;
    public static final int MAX_TEXTS_PER_DEFINITION = 128;
    public static final int MAX_TEXT_RULES_PER_DEFINITION = 64;
    public static final int MAX_ANIMATION_FRAMES = 512;
    public static final int MAX_SLOT_RANGE = 4096;
    public static final int MAX_PATH_LENGTH = 512;
    public static final int MAX_JSON_NESTING = 32;
    public static final int MAX_IMAGE_DIMENSION = 4096;
    public static final long MAX_IMAGE_PIXELS = 16L * 1024L * 1024L;
    public static final long MAX_ANIMATION_PIXELS = 64L * 1024L * 1024L;
    public static final long MAX_RELOAD_PIXELS = 128L * 1024L * 1024L;
    public static final double MAX_GUI_MAGNITUDE = 65536.0;

    private ResourceLimits() {
    }

    public static int boundedInt(double value, String field) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value != Math.rint(value)
                || Math.abs(value) > MAX_GUI_MAGNITUDE) {
            throw new IllegalArgumentException(field + " must be a finite integer within GUI limits");
        }
        return (int) value;
    }

    public static double finiteGui(double value, String field) {
        if (Double.isNaN(value) || Double.isInfinite(value)
                || Math.abs(value) > MAX_GUI_MAGNITUDE) {
            throw new IllegalArgumentException(field + " must be finite and within GUI limits");
        }
        return value;
    }

    public static String safePath(String value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        String normalized = value.trim().replace('\\', '/');
        if (normalized.isEmpty() || normalized.length() > MAX_PATH_LENGTH
                || normalized.startsWith("/") || normalized.matches("^[A-Za-z]:.*")) {
            throw new IllegalArgumentException(field + " is not a safe resource path: " + value);
        }
        String path = normalized.indexOf(':') >= 0
                ? normalized.substring(normalized.indexOf(':') + 1)
                : normalized;
        for (String segment : path.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw new IllegalArgumentException(field + " is not a safe resource path: " + value);
            }
        }
        return normalized;
    }
}

