package io.github.vialdevelopment.guerrilla.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This goes above a method with arguments
 * {@link org.objectweb.asm.tree.MethodNode}
 * {@link Boolean} can (and should be) primitive
 *
 * @author cats
 * @since October 4, 2020
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TransformMethod {

    /**
     * @return the name of the method
     */
    String methodName();

    /**
     * @return the description of the method
     */
    String methodDesc();

    /**
     * @return the obfuscated name of the method
     */
    String obfMethodName() default "";

    /**
     * @return the obfuscated description of the method
     *
     * Default allows for obfMethodDesc to not be entered
     * This should be impl'd by checking if the string is empty
     */
    String obfMethodDesc() default "";
}
