package io.github.vialdevelopment.guerrilla.annotation.insert;

import io.github.vialdevelopment.guerrilla.ASMFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <b>MUST</b> be declared with an
 * {@link io.github.vialdevelopment.guerrilla.annotation.TransformMethod annotation}
 * on it
 *
 *
 * @author cats
 * @since December 26, 2020
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Insert {

    /**
     * @return the {@link At} that determines where to insert
     */
    At value();

    /**
     * @return the type of method
     */
    ASMFactory.EReturnTypes returnType() default ASMFactory.EReturnTypes.RETURN;
}
