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
     *
     * @return method call, in format Opcode owner name desc, only for INVOKE
     * eg INVOKEVIRTUAL net/minecraft/entity/Entity addVelocity (DDD)V
     */
    String methodCall() default "";

    /**
     *
     * @return method call, in format Opcode owner name desc, only for INVOKE
     * eg INVOKEVIRTUAL ffa g (DDD)V
     */
    String obfMethodCall() default "";

    /**
     *
     * @return field reference, in format Opcode owner name desc, only for FIELD
     * eg GETFIELD net/minecraft/entity/player/EntityPlayer inventory Lnet/minecraft/entity/player/InventoryPlayer;
     */
    String fieldRef() default "";

    /**
     *
     * @return field reference, in format Opcode owner name desc, only for FIELD
     * eg  GETFIELD net/minecraft/entity/player/EntityPlayer inventory Lnet/minecraft/entity/player/InventoryPlayer;
     */
    String obfFieldRef() default "";

    /**
     * @return offset from target, only for INVOKE
     */
    int offset() default 0;

    /**
     * @return number of redirects to redirect
     */
    int numberMatch() default 0;
}
