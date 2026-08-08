package redfoxexpand.platform.forge1710;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Version-owned aliases for resource packs authored with modern menu names. */
public final class Forge1710ClassAliases {

    private static final Map<String, String> ALIASES;

    static {
        Map<String, String> aliases = new LinkedHashMap<String, String>();
        aliases.put("InventoryMenu", "net.minecraft.inventory.ContainerPlayer");
        aliases.put("ChestMenu", "net.minecraft.inventory.ContainerChest");
        aliases.put("CraftingMenu", "net.minecraft.inventory.ContainerWorkbench");
        aliases.put("FurnaceMenu", "net.minecraft.inventory.ContainerFurnace");
        aliases.put("AnvilMenu", "net.minecraft.inventory.ContainerRepair");
        aliases.put("MerchantMenu", "net.minecraft.inventory.ContainerMerchant");
        aliases.put("BrewingStandMenu", "net.minecraft.inventory.ContainerBrewingStand");
        aliases.put("HopperMenu", "net.minecraft.inventory.ContainerHopper");
        aliases.put("DispenserMenu", "net.minecraft.inventory.ContainerDispenser");
        aliases.put("BeaconMenu", "net.minecraft.inventory.ContainerBeacon");
        aliases.put("EnchantmentMenu", "net.minecraft.inventory.ContainerEnchantment");
        aliases.put("HorseInventoryMenu", "net.minecraft.inventory.ContainerHorseInventory");
        ALIASES = Collections.unmodifiableMap(aliases);
    }

    private Forge1710ClassAliases() {
    }

    public static String alias(String configuredName) {
        String simple = simpleName(configuredName);
        String mapped = ALIASES.get(simple);
        return mapped == null ? configuredName : mapped;
    }

    private static String simpleName(String name) {
        int separator = name.lastIndexOf('.');
        return separator < 0 ? name : name.substring(separator + 1);
    }
}
