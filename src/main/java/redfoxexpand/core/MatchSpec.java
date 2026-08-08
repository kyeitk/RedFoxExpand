package redfoxexpand.core;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/** Closed matcher model; no reflection or class loading occurs during matching. */
public sealed interface MatchSpec permits MatchSpec.All, MatchSpec.Any, MatchSpec.Not,
        MatchSpec.ExactScreenClass, MatchSpec.AssignableScreenClass,
        MatchSpec.ExactScreenSimpleClass, MatchSpec.AssignableScreenSimpleClass,
        MatchSpec.ExactMenuClass, MatchSpec.AssignableMenuClass,
        MatchSpec.ExactMenuSimpleClass, MatchSpec.AssignableMenuSimpleClass,
        MatchSpec.ScreenTitleKey, MatchSpec.ScreenTitleText,
        MatchSpec.MenuType, MatchSpec.ResourceLocation, MatchSpec.ModNamespace {

    boolean matches(GuiContext context);

    record All(List<MatchSpec> terms) implements MatchSpec {
        public All { terms = List.copyOf(terms); }
        @Override public boolean matches(GuiContext context) {
            return terms.stream().allMatch(term -> term.matches(context));
        }
    }

    record Any(List<MatchSpec> terms) implements MatchSpec {
        public Any { terms = List.copyOf(terms); }
        @Override public boolean matches(GuiContext context) {
            return terms.stream().anyMatch(term -> term.matches(context));
        }
    }

    record Not(MatchSpec term) implements MatchSpec {
        public Not { Objects.requireNonNull(term, "term"); }
        @Override public boolean matches(GuiContext context) { return !term.matches(context); }
    }

    record ExactScreenClass(String name) implements MatchSpec {
        @Override public boolean matches(GuiContext context) { return name.equals(context.screenClass()); }
    }

    record AssignableScreenClass(String name) implements MatchSpec {
        @Override public boolean matches(GuiContext context) { return context.screenHierarchy().contains(name); }
    }

    record ExactScreenSimpleClass(String name) implements MatchSpec {
        @Override public boolean matches(GuiContext context) { return simple(context.screenClass()).equals(name); }
    }

    record AssignableScreenSimpleClass(String name) implements MatchSpec {
        @Override public boolean matches(GuiContext context) {
            return context.screenHierarchy().stream().map(MatchSpec::simple).anyMatch(name::equals);
        }
    }

    record ExactMenuClass(String name) implements MatchSpec {
        @Override public boolean matches(GuiContext context) { return name.equals(context.menuClass()); }
    }

    record AssignableMenuClass(String name) implements MatchSpec {
        @Override public boolean matches(GuiContext context) { return context.menuHierarchy().contains(name); }
    }

    record ExactMenuSimpleClass(String name) implements MatchSpec {
        @Override public boolean matches(GuiContext context) { return simple(context.menuClass()).equals(name); }
    }

    record AssignableMenuSimpleClass(String name) implements MatchSpec {
        @Override public boolean matches(GuiContext context) {
            return context.menuHierarchy().stream().map(MatchSpec::simple).anyMatch(name::equals);
        }
    }

    record ScreenTitleKey(String value) implements MatchSpec {
        @Override public boolean matches(GuiContext context) { return value.equals(context.screenTitleKey()); }
    }

    record ScreenTitleText(String value, Pattern pattern) implements MatchSpec {
        public ScreenTitleText(String value) { this(value, compileGlob(value)); }
        @Override public boolean matches(GuiContext context) {
            return context.screenTitleText() != null && pattern.matcher(context.screenTitleText()).matches();
        }
    }

    record MenuType(String value) implements MatchSpec {
        @Override public boolean matches(GuiContext context) { return value.equals(context.menuType()); }
    }

    record ResourceLocation(String value) implements MatchSpec {
        @Override public boolean matches(GuiContext context) { return value.equals(context.resourceLocation()); }
    }

    record ModNamespace(String value) implements MatchSpec {
        @Override public boolean matches(GuiContext context) { return value.equals(context.modNamespace()); }
    }

    private static Pattern compileGlob(String value) {
        StringBuilder regex = new StringBuilder("^");
        String[] parts = value.split("\\*", -1);
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) regex.append(".*");
            regex.append(Pattern.quote(parts[i]));
        }
        return Pattern.compile(regex.append('$').toString());
    }

    private static String simple(String value) {
        if (value == null) return "";
        int split = value.lastIndexOf('.');
        return split < 0 ? value : value.substring(split + 1);
    }
}
