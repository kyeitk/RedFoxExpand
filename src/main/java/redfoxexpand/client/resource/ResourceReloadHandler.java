package redfoxexpand.client.resource;

import redfoxexpand.RedFoxExpand;
import redfoxexpand.client.gui.GuiModifierManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;

public final class ResourceReloadHandler implements IResourceManagerReloadListener {

    private final GuiModifierManager guiModifiers;

    public ResourceReloadHandler(GuiModifierManager guiModifiers) {
        this.guiModifiers = guiModifiers;
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        try {
            guiModifiers.reload(resourceManager);
        } catch (Throwable error) {
            RedFoxExpand.LOGGER.error(
                    "Failed to reload GUI modifiers; the previous immutable generation remains active",
                    error
            );
        }
    }
}
