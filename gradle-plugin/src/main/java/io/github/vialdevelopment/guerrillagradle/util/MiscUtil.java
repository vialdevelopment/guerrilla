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
    public static boolean isPublicMinecraft(String internalName) {
        return internalName.startsWith("net/minecraft/") && internalName.endsWith("PUBLIC");
    }

    public static boolean isValidToPublicName(String internalName) {
        return internalName.startsWith("net/minecraft/");
    }

    /**
     * Class name to valid minecraft-public class name
     * @param internalName class name
     * @return minecraft-public name
     */
    public static String toPublicMinecraft(String internalName) {
        return isValidToPublicName(internalName) ? internalName + "PUBLIC" : internalName;
    }

    public static String toNormalMinecraft(String internalName) {
        return isValidToPublicName(internalName) ? internalName.substring(0, internalName.length()-6) : internalName;
    }

}
