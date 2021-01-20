package io.github.vialdevelopment.guerrillagradle.util;

/**
 * Miscellaneous utilities
 */
public class MiscUtil {

    /**
     * Checks if this class name is a reference to a minecraft-public
     * @param internalName class name
     * @return is reference
     */
    public static boolean isPublicName(String internalName) {
        return internalName.startsWith("net/minecraft/") && internalName.endsWith("PUBLIC");
    }

    public static boolean isValidPublicName(String internalName) {
        return internalName.startsWith("net/minecraft/");
    }

    /**
     * Class name to valid minecraft-public class name
     * @param internalName class name
     * @return minecraft-public name
     */
    public static String toPublicName(String internalName) {
        return isValidPublicName(internalName) ? internalName + "PUBLIC" : internalName;
    }

    public static String toNormalName(String internalName) {
        return isValidPublicName(internalName) ? internalName.substring(0, internalName.length()-6) : internalName;
    }

}
