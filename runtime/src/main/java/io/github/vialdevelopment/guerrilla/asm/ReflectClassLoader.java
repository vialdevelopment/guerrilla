package io.github.vialdevelopment.guerrilla.asm;

import io.github.vialdevelopment.guerrilla.TransformManager;

/**
 * A small class loader to let us load class for our method reflection
 * This class loader is a child of our parent, for example launch wrapper
 */
public class ReflectClassLoader extends ClassLoader {

    public static ReflectClassLoader INSTANCE = new ReflectClassLoader(TransformManager.CLASS_LOADER);

    public ReflectClassLoader(ClassLoader classLoader) {
        super(classLoader);
    }

    public Class define(String name, byte[] bytes) {
        return defineClass(name, bytes, 0, bytes.length);
    }

}
