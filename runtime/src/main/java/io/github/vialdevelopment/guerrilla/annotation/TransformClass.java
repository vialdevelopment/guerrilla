package io.github.vialdevelopment.guerrilla.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This should be used to declare a class that is going to have asm transformations on it
 *
 * A class annotated with this will contain {@link TransformMethod}
 *
 * @author cats
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TransformClass {

    /**
     * @return the name of the class to transform
     */
    String name();

    /**
     * @return the obfuscated name of the class!
     */
    String obfName() default "";
}
