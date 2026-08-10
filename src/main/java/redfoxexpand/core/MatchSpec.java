package redfoxexpand.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/** Closed matcher model; no reflection or class loading occurs during matching. */
public interface MatchSpec {
    boolean matches(GuiContext context);

    abstract class Terms implements MatchSpec {
        final List<MatchSpec> terms;
        Terms(List<MatchSpec> terms) {
            this.terms = Collections.unmodifiableList(new ArrayList<MatchSpec>(terms));
        }
    }
    final class All extends Terms {
        public All(List<MatchSpec> terms) { super(terms); }
        public boolean matches(GuiContext context) {
            for (MatchSpec term : terms) if (!term.matches(context)) return false;
            return true;
        }
    }
    final class Any extends Terms {
        public Any(List<MatchSpec> terms) { super(terms); }
        public boolean matches(GuiContext context) {
            for (MatchSpec term : terms) if (term.matches(context)) return true;
            return false;
        }
    }
    final class Not implements MatchSpec {
        private final MatchSpec term;
        public Not(MatchSpec term) {
            if (term == null) throw new NullPointerException("term");
            this.term = term;
        }
        public boolean matches(GuiContext context) { return !term.matches(context); }
    }

    abstract class NameMatcher implements MatchSpec {
        final String name;
        NameMatcher(String name) { this.name = name; }
    }
    final class ExactScreenClass extends NameMatcher {
        public ExactScreenClass(String name) { super(name); }
        public boolean matches(GuiContext c) { return name.equals(c.screenClass()); }
    }
    final class AssignableScreenClass extends NameMatcher {
        public AssignableScreenClass(String name) { super(name); }
        public boolean matches(GuiContext c) { return c.screenHierarchy().contains(name); }
    }
    final class ExactScreenSimpleClass extends NameMatcher {
        public ExactScreenSimpleClass(String name) { super(name); }
        public boolean matches(GuiContext c) { return simple(c.screenClass()).equals(name); }
    }
    final class AssignableScreenSimpleClass extends NameMatcher {
        public AssignableScreenSimpleClass(String name) { super(name); }
        public boolean matches(GuiContext c) {
            for (String actual : c.screenHierarchy()) if (simple(actual).equals(name)) return true;
            return false;
        }
    }
    final class ExactMenuClass extends NameMatcher {
        public ExactMenuClass(String name) { super(compatMenu(name)); }
        public boolean matches(GuiContext c) { return name.equals(c.menuClass()); }
    }
    final class AssignableMenuClass extends NameMatcher {
        public AssignableMenuClass(String name) { super(compatMenu(name)); }
        public boolean matches(GuiContext c) { return c.menuHierarchy().contains(name); }
    }
    final class ExactMenuSimpleClass extends NameMatcher {
        public ExactMenuSimpleClass(String name) { super(compatMenu(name)); }
        public boolean matches(GuiContext c) { return simple(c.menuClass()).equals(name); }
    }
    final class AssignableMenuSimpleClass extends NameMatcher {
        public AssignableMenuSimpleClass(String name) { super(compatMenu(name)); }
        public boolean matches(GuiContext c) {
            for (String actual : c.menuHierarchy()) if (simple(actual).equals(name)) return true;
            return false;
        }
    }
    final class ScreenTitleKey extends NameMatcher {
        public ScreenTitleKey(String value) { super(value); }
        public boolean matches(GuiContext c) { return name.equals(c.screenTitleKey()); }
    }
    final class ScreenTitleText extends NameMatcher {
        private final Pattern pattern;
        public ScreenTitleText(String value) { super(value); this.pattern = compileGlob(value); }
        public boolean matches(GuiContext c) {
            return c.screenTitleText() != null && pattern.matcher(c.screenTitleText()).matches();
        }
    }
    final class MenuType extends NameMatcher {
        public MenuType(String value) { super(value); }
        public boolean matches(GuiContext c) { return name.equals(c.menuType()); }
    }
    final class ResourceLocation extends NameMatcher {
        public ResourceLocation(String value) { super(value); }
        public boolean matches(GuiContext c) { return name.equals(c.resourceLocation()); }
    }
    final class ModNamespace extends NameMatcher {
        public ModNamespace(String value) { super(value); }
        public boolean matches(GuiContext c) { return name.equals(c.modNamespace()); }
    }

    final class Helpers {
        private Helpers() { }
        static Pattern compileGlob(String value) {
            StringBuilder regex = new StringBuilder("^");
            String[] parts = value.split("\\*", -1);
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) regex.append(".*");
                regex.append(Pattern.quote(parts[i]));
            }
            return Pattern.compile(regex.append('$').toString());
        }
        static String simple(String value) {
            if (value == null) return "";
            int split = value.lastIndexOf('.');
            return split < 0 ? value : value.substring(split + 1);
        }
        static String compatMenu(String value) {
            if ("net.minecraft.world.inventory.InventoryMenu".equals(value)) {
                return "net.minecraft.inventory.ContainerPlayer";
            }
            if ("InventoryMenu".equals(value)) return "ContainerPlayer";
            return value;
        }
    }

    static Pattern compileGlob(String value) { return Helpers.compileGlob(value); }
    static String simple(String value) { return Helpers.simple(value); }
    static String compatMenu(String value) { return Helpers.compatMenu(value); }
}
