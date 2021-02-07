package io.github.vialdevelopment.guerrilla.annotation.info;

/**
 * An annotation to tell us when a transformer class is extending something
 *
 * This is done because its super class is stripped to java/lang/Object to prevent class circularity issues.
 * Should only be inserted by the gradle plugin
 */
public @interface TransformerExtends {

    /**
     * @return class this annotation class extended originally
     */
    String clazz();

}
