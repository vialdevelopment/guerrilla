package io.github.vialdevelopment.guerrilla.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Transforms the access of a field
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TransformFieldAccess {
    /**
     * use Opcodes.ACC_*
     * @return type of access
     */
    int access();

    /**
     * @return name of field
     */
    String name();

    /**
     * @return obf name of field
     */
    String obfName() default "";


}
