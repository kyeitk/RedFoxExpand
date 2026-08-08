package redfoxexpand.platform.forge1710;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Semantic vanilla title keys owned by the 1.7.10 platform adapter. */
public final class Forge1710TitleAliases {

    private static final Map<String, List<String>> KEYS;

    static {
        Map<String, List<String>> keys = new LinkedHashMap<String, List<String>>();
        keys.put("GuiCrafting", one("container.crafting"));
        keys.put("GuiRepair", one("container.repair"));
        keys.put("GuiMerchant", one("entity.Villager.name"));
        keys.put("GuiEnchantment", one("container.enchant"));
        keys.put("GuiBrewingStand", one("container.brewing"));
        keys.put("GuiBeacon", one("container.beacon"));
        keys.put("GuiHopper", one("container.hopper"));
        KEYS = Collections.unmodifiableMap(keys);
    }

    private Forge1710TitleAliases() {
    }

    public static List<String> keysFor(String screenSimpleName) {
        List<String> keys = KEYS.get(screenSimpleName);
        return keys == null ? Collections.<String>emptyList() : keys;
    }

    private static List<String> one(String value) {
        return Collections.unmodifiableList(Arrays.asList(value));
    }
}
