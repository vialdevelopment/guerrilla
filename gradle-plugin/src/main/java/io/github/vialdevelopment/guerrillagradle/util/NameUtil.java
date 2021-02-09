package io.github.vialdevelopment.guerrillagradle.util;

import java.util.List;

/**
 * Name utilities
 */
public class NameUtil {

    /**
     * Checks if this class name is a reference to a minecraft-public
     * @param internalName class name
     * @return is reference
     */
    public static boolean isPublicName(String internalName, List<String> makePublics) {
        return isValidPublicName(internalName, makePublics) && internalName.endsWith("PUBLIC");
    }

    public static boolean isValidPublicName(String internalName, List<String> makePublics) {
        final boolean[] transform = new boolean[] {false};
        for (String aPublic : makePublics) {
            if (internalName.matches(aPublic)) {
                transform[0] = true;
                break;
            }
        }
        return transform[0];
        }

    /**
     * Class name to valid public class name
     * @param internalName class name
     * @return minecraft-public name
     */
    public static String toPublicName(String internalName, List<String> makePublics) {
        return isValidPublicName(internalName, makePublics) ? internalName + "PUBLIC" : internalName;
    }

    public static String toNormalName(String internalName, List<String> makePublics) {
        return isPublicName(internalName, makePublics) ? internalName.substring(0, internalName.length()-6) : internalName;
    }

}
