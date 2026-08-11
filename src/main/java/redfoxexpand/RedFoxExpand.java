package redfoxexpand;

import redfoxexpand.client.gui.GuiEventHandler;
import redfoxexpand.client.gui.GuiModifierManager;
import redfoxexpand.client.gui.ReactiveTickHandler;
import redfoxexpand.client.resource.ResourceReloadHandler;
import redfoxexpand.client.render.GuiTextureRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
        modid = RedFoxExpand.MOD_ID,
        name = RedFoxExpand.NAME,
        version = RedFoxExpand.VERSION,
        acceptedMinecraftVersions = "[1.7.10]",
        acceptableRemoteVersions = "*"
)
public final class RedFoxExpand {

    public static final String MOD_ID = "redfoxexpand";
    public static final String NAME = "RedFoxExpand";
    public static final String VERSION = "0.2.0";

    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static final GuiModifierManager GUI_MODIFIERS = new GuiModifierManager();
    public static final GuiTextureRenderer GUI_TEXTURE_RENDERER = new GuiTextureRenderer();

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new GuiEventHandler(GUI_MODIFIERS));
        FMLCommonHandler.instance().bus().register(new ReactiveTickHandler());

        if (!(Minecraft.getMinecraft().getResourceManager() instanceof IReloadableResourceManager)) {
            throw new IllegalStateException("The active client resource manager is not reloadable");
        }

        IReloadableResourceManager resourceManager =
                (IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager();
        resourceManager.registerReloadListener(new ResourceReloadHandler(GUI_MODIFIERS));
    }
}
