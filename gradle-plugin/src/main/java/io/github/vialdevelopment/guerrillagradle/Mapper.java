package io.github.vialdevelopment.guerrillagradle;

import io.github.vialdevelopment.guerrillagradle.util.NameTreeSet;
import org.gradle.api.Project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

/**
 * The Mapper is used to load mappings and called to obtain obfuscated names
 */
public class Mapper {
    /** The folder containing the mcp mappings */
    public Path mcpFolder;
    /** A hashmap containing the mappings from unObf -> obf */
    public Map<String, String> unObfToObfMappings = new HashMap<>();
    /** A NameTreeSet of the inheritance structure of the minecraft classes */
    public NameTreeSet inheritanceTree;

    /**
     * Initializes the mapper to be ready to query
     * @param project gradle project
     * @param mcpVersion mcp version
     */
    public void init(Project project, String mcpVersion) {
        if (mcpVersion.equals("")) { // make sure mcp version is set
            System.out.println("MCP VERSION NOT SET");
            return;
        }
        // TODO check if on forge
        // get the folder containing the mcp mappings
        String mcpType = "mcp_" + mcpVersion.split("_")[0];
        String mcpVersionNumber = mcpVersion.split("_")[1];
        mcpFolder = Paths.get(Paths.get(project.getGradle().getGradleUserHomeDir().getPath(), "caches/minecraft/de/oceanlabs/mcp/", mcpType + "/" + mcpVersionNumber).toString());
        // load the mappings
        loadMappings();
    }

    /**
     * Loads the mappings,
     */
    public void loadMappings() {
        try {
            // get the file mcp-notch.srg
            File mcpToNotchSrgFile = null;
            // only 1 folder in mcp folder
            for (File file : Objects.requireNonNull(mcpFolder.toFile().listFiles())) {
                if (file.isDirectory()) {
                    mcpToNotchSrgFile = new File(file.getPath() + "/srgs/mcp-notch.srg");
                    break;
                }
            }
            assert mcpToNotchSrgFile != null;
            // read in the mappings
            FileInputStream inputStream = new FileInputStream(mcpToNotchSrgFile);
            Scanner scanner = new Scanner(inputStream, "UTF-8");
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] lineSplitSpaces = line.split(" ");
                switch(line.substring(0, 2)) {
                    case "CL":
                        unObfToObfMappings.put(lineSplitSpaces[1], lineSplitSpaces[2]);
                        break;
                    case "FD":
                        unObfToObfMappings.put(lineSplitSpaces[1], lineSplitSpaces[2]);
                        break;
                    case "MD":
                        unObfToObfMappings.put(lineSplitSpaces[1] + " " + lineSplitSpaces[2], lineSplitSpaces[3] + " " + lineSplitSpaces[4]);
                        break;
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private enum remap {
        CLASS,
        METHOD_NAME,
        FIELD_ACCESS,
        METHOD_ACCESS
    }

    private void handleUnmapped(remap type, String unmappedName) {
        System.out.println("Failed to map " + type.name() + ": " + unmappedName);
    }

    /**
     * Returns the obf name of the class
     * ie net/minecraft/blah -> net/minecraft/aa
     * @param className class un-obf name
     * @return
     */
    public String remapClassName(String className) {
        String remapped = unObfToObfMappings.get(className.replace('.', '/'));
        if (remapped == null) {
            handleUnmapped(remap.CLASS, className);
            return className;
        }
        return remapped;
    }

    /**
     * Returns the obf name of the method
     * @param className owner class of method
     * @param methodName method un-obf name
     * @param methodArgs arguments of method
     * @return obf name
     */
    public String remapMethodName(String className, String methodName, String methodArgs) {
        className = className.replace('.', '/');
        String found = unObfToObfMappings.get(className + "/" + methodName + " " + methodArgs);

        NameTreeSet tree = inheritanceTree.contains(className);
        while (found == null && tree != null) {
            found = unObfToObfMappings.get(tree.name + "/" + methodName + " " + methodArgs);
            tree = tree.superTree;
        }

        if (found == null) {
            handleUnmapped(remap.METHOD_NAME, className + "/" + methodName + " " + methodArgs);
            return className + " " + methodName + " " + methodArgs;
        }

        String[] wholeSplit = found.split(" ");

        return wholeSplit[0].substring(wholeSplit[0].lastIndexOf("/")+1) + " " + wholeSplit[1];
    }

    /**
     * Returns the obf field access
     * @param className owner class of the field
     * @param fieldName field un-obf name
     * @return obf access
     */
    public String remapFieldAccess(String className, String fieldName) {
        className = className.replace('.', '/');
        String found = unObfToObfMappings.get(className + "/" + fieldName);

        NameTreeSet tree = inheritanceTree.contains(className);
        while (found == null && tree != null) {
            found = unObfToObfMappings.get(tree.name + "/" + fieldName);
            tree = tree.superTree;
        }

        if (found == null) {
            handleUnmapped(remap.FIELD_ACCESS, className + "/" + fieldName);
            return className + "/" + fieldName;
        }

        return found;
    }

    /**
     * Returns the obf method access
     * @param className owner class of method
     * @param methodName method un-obf name
     * @param methodSignature signature of method
     * @return obf access
     */
    public String remapMethodAccess(String className, String methodName, String methodSignature) {
        String found = unObfToObfMappings.get(className + "/" + methodName + " " + methodSignature);

        NameTreeSet tree = inheritanceTree.contains(className);
        while (found == null && tree != null) {
            found = unObfToObfMappings.get(tree.name + "/" + methodName + " " + methodSignature);
            tree = tree.superTree;
        }

        if (found == null) {
            handleUnmapped(remap.METHOD_ACCESS, className + "/" + methodName + " " + methodSignature);
            return className + "/" + methodName + " " + methodSignature;
        }

        return found;
    }

}
