package redfoxexpand.client.gui;

public interface GuiModifierScreenAccess {

    ResolvedGuiModifier redfoxexpand$getModifier();

    void redfoxexpand$onPostInit();

    void redfoxexpand$refreshModifier();

    void redfoxexpand$afterInventoryEffectOriginUpdate();

    int redfoxexpand$getPotionEffectsX();
}
