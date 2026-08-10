package redfoxexpand.client.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import redfoxexpand.client.compat.LegacyResourceAdapter;
import redfoxexpand.client.config.GuiTextureResolver;
import redfoxexpand.client.render.GuiTexture;
import redfoxexpand.core.DefinitionCandidate;
import redfoxexpand.core.GuiDefinition;
import net.minecraft.util.ResourceLocation;

import java.util.Locale;
import java.util.Map;

public final class SpriteOverlay {

    public enum Anchor {
        GUI,
        SCREEN_CENTER,
        SCREEN;

        static Anchor parse(String value) {
            String normalized = value.toLowerCase(Locale.ROOT);
            if ("gui".equals(normalized) || "gui_origin".equals(normalized)) {
                return GUI;
            }
            if ("screen_center".equals(normalized) || "center".equals(normalized)) {
                return SCREEN_CENTER;
            }
            if ("screen".equals(normalized)
                    || "screen_top_left".equals(normalized)
                    || "absolute".equals(normalized)) {
                return SCREEN;
            }
            throw new IllegalArgumentException("Unsupported sprite anchor: " + value);
        }
    }

    public enum Layer {
        UNDERLAY,
        BACKGROUND,
        FOREGROUND;

        static Layer parse(String value) {
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            if ("underlay".equals(normalized) || "behind".equals(normalized)) {
                return UNDERLAY;
            }
            if ("background".equals(normalized)) {
                return BACKGROUND;
            }
            if ("foreground".equals(normalized)) {
                return FOREGROUND;
            }
            throw new IllegalArgumentException(
                    "Sprite layer must be underlay, background or foreground: " + value
            );
        }
    }

    private enum ResourceType {
        RESOURCE_LOCATION,
        GUI_SPRITE,
        AUTO;

        static ResourceType parse(String value) {
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            if ("resource_location".equals(normalized)) {
                return RESOURCE_LOCATION;
            }
            if ("gui_sprite".equals(normalized)) {
                return GUI_SPRITE;
            }
            if ("auto".equals(normalized)) {
                return AUTO;
            }
            throw new IllegalArgumentException(
                    "Sprite resource_type must be resource_location, gui_sprite or auto: " + value
            );
        }
    }

    public final GuiTexture texture;
    public final float x;
    public final float y;
    public final float z;
    public final float u;
    public final float v;
    public final float width;
    public final float height;
    public final float sourceWidth;
    public final float sourceHeight;
    public final float textureWidth;
    public final float textureHeight;
    public final boolean fullTexture;
    public final int color;
    public final Layer layer;
    public final Anchor anchor;
    public final String elementId;
    public final DefinitionCandidate reactiveScope;

    private SpriteOverlay(
            GuiTexture texture,
            float x,
            float y,
            float z,
            float u,
            float v,
            float width,
            float height,
            float sourceWidth,
            float sourceHeight,
            float textureWidth,
            float textureHeight,
            boolean fullTexture,
            int color,
            Layer layer,
            Anchor anchor,
            String elementId,
            DefinitionCandidate reactiveScope
    ) {
        this.texture = texture;
        this.x = x;
        this.y = y;
        this.z = z;
        this.u = u;
        this.v = v;
        this.width = width;
        this.height = height;
        this.sourceWidth = sourceWidth;
        this.sourceHeight = sourceHeight;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.fullTexture = fullTexture;
        this.color = color;
        this.layer = layer;
        this.anchor = anchor;
        this.elementId = elementId;
        this.reactiveScope = reactiveScope;
    }

    public static SpriteOverlay parse(JsonObject json) {
        return parse(json, LegacyResourceAdapter.INSTANCE);
    }

    public static SpriteOverlay parse(JsonObject json, GuiTextureResolver textureResolver) {
        return parse(json, textureResolver, false);
    }

    private static SpriteOverlay parse(
            JsonObject json,
            GuiTextureResolver textureResolver,
            boolean animation
    ) {
        String texture = JsonSupport.string(json, "texture", "").trim();
        if (texture.isEmpty()) {
            throw new IllegalArgumentException("Sprite is missing texture");
        }
        ResourceLocation configuredTexture = new ResourceLocation(texture);
        ResourceType resourceType = ResourceType.parse(
                JsonSupport.string(json, "resource_type", "auto")
        );
        if (animation && resourceType == ResourceType.GUI_SPRITE) {
            throw new IllegalArgumentException(
                    "Animation texture cannot use resource_type gui_sprite"
            );
        }
        boolean guiAtlasId = resourceType == ResourceType.GUI_SPRITE
                || (resourceType == ResourceType.AUTO && isGuiAtlasId(configuredTexture));
        GuiTexture resolvedTexture = animation
                ? textureResolver.resolveAnimation(texture)
                : textureResolver.resolveStatic(texture, guiAtlasId);

        float x = JsonSupport.floatingAlias(json, "screen_x", "x", 0.0F);
        float y = JsonSupport.floatingAlias(json, "screen_y", "y", 0.0F);
        float z = JsonSupport.floating(json, "z", 0.0F);
        float width = JsonSupport.floating(json, "width", 16.0F);
        float height = JsonSupport.floating(json, "height", 16.0F);
        float sourceWidth = JsonSupport.floating(json, "source_width", width);
        float sourceHeight = JsonSupport.floating(json, "source_height", height);
        float textureWidth = JsonSupport.floatingAlias(json, "tex_width", "texture_width", 256.0F);
        float textureHeight = JsonSupport.floatingAlias(json, "tex_height", "texture_height", 256.0F);
        boolean hasRegion = json.has("u") || json.has("v")
                || json.has("source_width") || json.has("source_height")
                || json.has("tex_width") || json.has("tex_height")
                || json.has("texture_width") || json.has("texture_height");
        boolean fullTexture = JsonSupport.bool(json, "full_texture", !hasRegion);
        Layer layer = json.has("layer")
                ? Layer.parse(JsonSupport.string(json, "layer", "background"))
                : (z < 0.0F ? Layer.UNDERLAY : Layer.BACKGROUND);
        Anchor anchor = Anchor.parse(JsonSupport.string(
                json,
                "anchor",
                guiAtlasId ? "screen_center" : "gui"
        ));

        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Sprite width and height must be positive");
        }
        if (sourceWidth <= 0 || sourceHeight <= 0) {
            throw new IllegalArgumentException("Sprite source dimensions must be positive");
        }
        if (!fullTexture && (textureWidth <= 0 || textureHeight <= 0)) {
            throw new IllegalArgumentException("Sprite texture dimensions must be positive");
        }
        return new SpriteOverlay(
                resolvedTexture,
                x,
                y,
                z,
                JsonSupport.floating(json, "u", 0.0F),
                JsonSupport.floating(json, "v", 0.0F),
                width,
                height,
                sourceWidth,
                sourceHeight,
                textureWidth,
                textureHeight,
                fullTexture,
                0xFFFFFFFF,
                layer,
                anchor,
                null,
                null
        );
    }

    public static SpriteOverlay fromNative(
            GuiDefinition.Sprite sprite,
            GuiTexture texture,
            DefinitionCandidate scope
    ) {
        return new SpriteOverlay(
                texture,
                (float) sprite.x(), (float) sprite.y(), (float) sprite.z(),
                (float) sprite.u(), (float) sprite.v(),
                (float) sprite.width(), (float) sprite.height(),
                (float) sprite.sourceWidth(), (float) sprite.sourceHeight(),
                (float) sprite.textureWidth(), (float) sprite.textureHeight(),
                sprite.fullTexture(), sprite.color(),
                Layer.valueOf(sprite.layer().name()), Anchor.valueOf(sprite.anchor().name()),
                sprite.id(), scope
        );
    }

    public static SpriteOverlay parseCustomTexture(JsonObject json) {
        return parseCustomTexture(json, LegacyResourceAdapter.INSTANCE);
    }

    public static SpriteOverlay parseCustomTexture(
            JsonObject json,
            GuiTextureResolver textureResolver
    ) {
        JsonObject normalized = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            normalized.add(entry.getKey(), entry.getValue());
        }
        String textureType = JsonSupport.string(normalized, "texture_type", "full")
                .trim()
                .toLowerCase(Locale.ROOT);
        boolean animation = false;
        if ("full".equals(textureType)) {
            normalized.addProperty("full_texture", true);
        } else if ("region".equals(textureType)) {
            normalized.addProperty("full_texture", false);
            copyAlias(normalized, "image_x", "u");
            copyAlias(normalized, "image_y", "v");
            copyAlias(normalized, "image_width", "source_width");
            copyAlias(normalized, "image_height", "source_height");
            copyAlias(normalized, "texture_width", "tex_width");
            copyAlias(normalized, "texture_height", "tex_height");
            if (!normalized.has("texture_width") && !normalized.has("tex_width")) {
                throw new IllegalArgumentException("region texture_type requires texture_width");
            }
            if (!normalized.has("texture_height") && !normalized.has("tex_height")) {
                throw new IllegalArgumentException("region texture_type requires texture_height");
            }
        } else if ("animation".equals(textureType)) {
            animation = true;
            normalized.addProperty("full_texture", true);
        } else {
            throw new IllegalArgumentException(
                    "Custom texture_type must be full, region or animation: " + textureType
            );
        }

        if (!normalized.has("anchor")) {
            normalized.addProperty("anchor", "gui");
        }
        if (!normalized.has("resource_type")) {
            normalized.addProperty("resource_type", "resource_location");
        }
        return parse(normalized, textureResolver, animation);
    }

    private static void copyAlias(JsonObject json, String source, String target) {
        if (json.has(source) && !json.has(target)) {
            json.add(target, json.get(source));
        }
    }

    private static boolean isGuiAtlasId(ResourceLocation texture) {
        String path = texture.getResourcePath();
        return !path.startsWith("textures/") && !path.endsWith(".png");
    }

    public boolean isUnderlay() {
        return layer == Layer.UNDERLAY;
    }

    public boolean isBackground() {
        return layer == Layer.BACKGROUND;
    }

    public boolean isForeground() {
        return layer == Layer.FOREGROUND;
    }

    public boolean isNative() {
        return reactiveScope != null;
    }

    public float resolveRenderX(int guiLeft, int screenWidth, boolean matrixAtGuiOrigin) {
        float originX;
        if (anchor == Anchor.SCREEN_CENTER) {
            originX = screenWidth / 2.0F;
        } else if (anchor == Anchor.SCREEN) {
            originX = 0.0F;
        } else {
            originX = guiLeft;
        }
        if (matrixAtGuiOrigin) {
            originX -= guiLeft;
        }
        return originX + x;
    }

    public float resolveRenderY(int guiTop, int screenHeight, boolean matrixAtGuiOrigin) {
        float originY;
        if (anchor == Anchor.SCREEN_CENTER) {
            originY = screenHeight / 2.0F;
        } else if (anchor == Anchor.SCREEN) {
            originY = 0.0F;
        } else {
            originY = guiTop;
        }
        if (matrixAtGuiOrigin) {
            originY -= guiTop;
        }
        return originY + y;
    }

    public float minU() {
        return fullTexture ? 0.0F : u / textureWidth;
    }

    public float minV() {
        return fullTexture ? 0.0F : v / textureHeight;
    }

    public float maxU() {
        return fullTexture ? 1.0F : (u + sourceWidth) / textureWidth;
    }

    public float maxV() {
        return fullTexture ? 1.0F : (v + sourceHeight) / textureHeight;
    }

}
