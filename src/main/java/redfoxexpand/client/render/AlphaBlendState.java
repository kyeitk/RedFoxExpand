package redfoxexpand.client.render;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.nio.FloatBuffer;

/** Captures and restores every OpenGL state changed by GUI texture drawing. */
public final class AlphaBlendState implements AutoCloseable {

    private static final int GL_GET_FLOAT_BUFFER_SIZE = 16;

    private final boolean blendEnabled;
    private final boolean alphaEnabled;
    private final boolean depthEnabled;
    private final boolean scissorEnabled;
    private final boolean textureEnabled;
    private final int boundTexture;
    private final int blendSourceRgb;
    private final int blendDestinationRgb;
    private final int blendSourceAlpha;
    private final int blendDestinationAlpha;
    private final int alphaFunction;
    private final float alphaReference;
    private final float red;
    private final float green;
    private final float blue;
    private final float alpha;
    private boolean restored;

    private AlphaBlendState() {
        blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        alphaEnabled = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        depthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        scissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        textureEnabled = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        boundTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        blendSourceRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        blendDestinationRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        blendSourceAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        blendDestinationAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        alphaFunction = GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC);
        alphaReference = GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);

        FloatBuffer color = createGlGetFloatQueryBuffer();
        GL11.glGetFloat(GL11.GL_CURRENT_COLOR, color);
        red = color.get(0);
        green = color.get(1);
        blue = color.get(2);
        alpha = color.get(3);
    }

    public static AlphaBlendState begin() {
        AlphaBlendState state = new AlphaBlendState();
        if (state.scissorEnabled) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendFuncSeparate(
                GL11.GL_SRC_ALPHA,
                GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE,
                GL11.GL_ZERO
        );
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        return state;
    }

    static FloatBuffer createGlGetFloatQueryBuffer() {
        // LWJGL 2 validates glGetFloat buffers against its largest possible result (a 4x4 matrix),
        // even when the requested value such as GL_CURRENT_COLOR contains only four floats.
        return BufferUtils.createFloatBuffer(GL_GET_FLOAT_BUFFER_SIZE);
    }

    @Override
    public void close() {
        if (restored) {
            return;
        }
        restored = true;

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, boundTexture);
        GL14.glBlendFuncSeparate(
                blendSourceRgb,
                blendDestinationRgb,
                blendSourceAlpha,
                blendDestinationAlpha
        );
        GL11.glAlphaFunc(alphaFunction, alphaReference);
        GL11.glColor4f(red, green, blue, alpha);

        restoreManagedState(textureEnabled, new StateToggle() {
            @Override
            public void enable() {
                GL11.glEnable(GL11.GL_TEXTURE_2D);
            }

            @Override
            public void disable() {
                GL11.glDisable(GL11.GL_TEXTURE_2D);
            }
        });
        restoreManagedState(blendEnabled, new StateToggle() {
            @Override
            public void enable() {
                GL11.glEnable(GL11.GL_BLEND);
            }

            @Override
            public void disable() {
                GL11.glDisable(GL11.GL_BLEND);
            }
        });
        restoreManagedState(alphaEnabled, new StateToggle() {
            @Override
            public void enable() {
                GL11.glEnable(GL11.GL_ALPHA_TEST);
            }

            @Override
            public void disable() {
                GL11.glDisable(GL11.GL_ALPHA_TEST);
            }
        });
        restoreManagedState(depthEnabled, new StateToggle() {
            @Override
            public void enable() {
                GL11.glEnable(GL11.GL_DEPTH_TEST);
            }

            @Override
            public void disable() {
                GL11.glDisable(GL11.GL_DEPTH_TEST);
            }
        });
        if (scissorEnabled) {
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
        } else {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    private static void restoreManagedState(boolean enabled, StateToggle toggle) {
        if (enabled) {
            toggle.enable();
        } else {
            toggle.disable();
        }
    }

    private interface StateToggle {
        void enable();

        void disable();
    }
}
