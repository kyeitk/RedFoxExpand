package redfoxexpand.platform.forge1710;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Isolates the legacy FML remapper dependency from the shared matcher. */
public final class Forge1710ClassRemapper {

    private Forge1710ClassRemapper() {
    }

    public static String remap(String className, String methodName) {
        try {
            Class<?> remapperClass = Class.forName(
                    "cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper"
            );
            Field instanceField = remapperClass.getField("INSTANCE");
            Object remapper = instanceField.get(null);
            Method method = remapperClass.getMethod(methodName, String.class);
            Object result = method.invoke(remapper, className.replace('.', '/'));
            return result == null ? null : result.toString().replace('/', '.');
        } catch (Throwable ignored) {
            return null;
        }
    }
}
