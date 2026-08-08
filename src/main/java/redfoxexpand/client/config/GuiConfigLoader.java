package redfoxexpand.client.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.util.ResourceLocation;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import redfoxexpand.client.resource.ResourceLimits;

/** Strict, file-atomic JSON loader for GUI definitions. */
public final class GuiConfigLoader {

    public List<GuiDefinition> load(
            ResourceLocation source,
            Reader reader,
            GuiTextureResolver textureResolver
    ) {
        JsonElement root = new JsonParser().parse(reader);
        List<GuiDefinition> definitions = new ArrayList<GuiDefinition>();
        if (root.isJsonArray()) {
            if (root.getAsJsonArray().size() > ResourceLimits.MAX_DEFINITIONS_PER_FILE) {
                throw new IllegalArgumentException("Too many GUI definitions in " + source);
            }
            int index = 0;
            for (JsonElement element : root.getAsJsonArray()) {
                definitions.add(GuiDefinition.parse(
                        source,
                        element.getAsJsonObject(),
                        textureResolver,
                        index++
                ));
            }
        } else {
            definitions.add(GuiDefinition.parse(
                    source,
                    root.getAsJsonObject(),
                    textureResolver,
                    0
            ));
        }
        return Collections.unmodifiableList(definitions);
    }
}
