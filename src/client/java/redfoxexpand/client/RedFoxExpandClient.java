package redfoxexpand.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redfoxexpand.client.gui.ScreenController;
import redfoxexpand.client.resource.RedFoxReloadListener;

public final class RedFoxExpandClient implements ClientModInitializer {
    public static final String MOD_ID = "redfoxexpand";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                Identifier.fromNamespaceAndPath(MOD_ID, "gui_definitions"),
                new RedFoxReloadListener()
        );
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> ScreenController.INSTANCE.attach(screen));
        LOGGER.info("RedFoxExpand initialized for Minecraft 26.2 Fabric");
    }
}
