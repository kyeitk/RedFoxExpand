package redfoxexpand.client.gui;

import java.util.Locale;

/** Controls whether a configured class name matches one class or its hierarchy. */
public enum ClassMatchMode {
    EXACT,
    ASSIGNABLE;

    public static ClassMatchMode parse(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("exact".equals(normalized)) {
            return EXACT;
        }
        if ("assignable".equals(normalized)) {
            return ASSIGNABLE;
        }
        throw new IllegalArgumentException(
                "Class match mode must be exact or assignable: " + value
        );
    }
}
