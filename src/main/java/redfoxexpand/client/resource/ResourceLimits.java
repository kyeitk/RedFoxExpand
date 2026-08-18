package redfoxexpand.client.resource;

import java.awt.image.BufferedImage;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Versioned safety budgets for untrusted resource-pack data. */
public final class ResourceLimits {

    public static final int MAX_CONFIG_BYTES = 1024 * 1024;
    public static final int MAX_ANIMATION_BYTES = 256 * 1024;
    public static final int MAX_DEFINITIONS_PER_FILE = 1024;
    public static final int MAX_ENTRIES_PER_LIST = 4096;
    public static final int MAX_DISCOVERED_RESOURCES = 16384;
    public static final int MAX_RESOURCE_PATH_LENGTH = 512;
    public static final int MAX_IMAGE_DIMENSION = 4096;
    public static final long MAX_IMAGE_PIXELS = Long.MAX_VALUE;
    public static final long MAX_GENERATION_PIXELS = Long.MAX_VALUE;
    public static final float MAX_GUI_MAGNITUDE = 65536.0F;

    private ResourceLimits() {
    }

    public static InputStream limited(InputStream source, long maximum, String label) {
        return new FilterInputStream(source) {
            private long count;

            @Override
            public int read() throws IOException {
                int value = super.read();
                if (value >= 0) {
                    consume(1);
                }
                return value;
            }

            @Override
            public int read(byte[] buffer, int offset, int length) throws IOException {
                int read = super.read(buffer, offset, length);
                if (read > 0) {
                    consume(read);
                }
                return read;
            }

            private void consume(int amount) throws IOException {
                count += amount;
                if (count > maximum) {
                    throw new IOException(label + " exceeds " + maximum + " bytes");
                }
            }
        };
    }

    public static long imagePixels(BufferedImage image, String label) {
        int width = image.getWidth();
        int height = image.getHeight();
        long pixels = (long) width * (long) height;
        if (width <= 0 || height <= 0
                || width > MAX_IMAGE_DIMENSION
                || height > MAX_IMAGE_DIMENSION
                || pixels > MAX_IMAGE_PIXELS) {
            throw new IllegalArgumentException(label + " exceeds the image budget");
        }
        return pixels;
    }

    public static float finiteGuiValue(float value, String field) {
        if (Float.isNaN(value) || Float.isInfinite(value)
                || Math.abs(value) > MAX_GUI_MAGNITUDE) {
            throw new IllegalArgumentException(field + " must be finite and within GUI limits");
        }
        return value;
    }
}
