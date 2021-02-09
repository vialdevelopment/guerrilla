package io.github.vialdevelopment.guerrilla.annotation.insert;

/**
 * Holds location of where to inject
 */
public @interface At {

    enum loc {
        HEAD,
        INVOKE,
        FIELD,
        RETURN
    }

    /**
     * @return Location
     */
    loc at();

    /**
     * @return ref, in format Opcode owner name desc, for INVOKE and FIELD
     * eg INVOKEVIRTUAL net/minecraft/entity/Entity addVelocity (DDD)V
     * eg GETFIELD net/minecraft/entity/player/EntityPlayer inventory Lnet/minecraft/entity/player/InventoryPlayer;
     */
    String ref() default "";

    /**
     * @return obfuscated ref
     */
    String obfRef() default "";

    /**
     * @return offset from target, only for INVOKE
     */
    int offset() default 0;
}
