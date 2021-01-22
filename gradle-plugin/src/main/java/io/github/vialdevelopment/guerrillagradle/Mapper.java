package io.github.vialdevelopment.guerrillagradle;

import org.gradle.api.Project;

import java.io.*;
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
    /** A hashmap of the inheritance structure of the minecraft classes */
    public Map<String, String> inheritanceTree;

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
        loadMappings(project);
    }

    /**
     * Loads the mappings,
     */
    public void loadMappings(Project project) {
        File mappingsFile = new File(project.getBuildDir() + "/tmp/guerrilla/mappings");

        if (mappingsFile.exists()) {
            try {
                FileInputStream fileInputStream = new FileInputStream(mappingsFile);
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                unObfToObfMappings = (Map<String, String>) objectInputStream.readObject();
                objectInputStream.close();
                fileInputStream.close();
                return;
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

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

            FileOutputStream fileOutputStream = new FileOutputStream(mappingsFile);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(unObfToObfMappings);
            objectOutputStream.close();
            fileOutputStream.close();

        } catch (IOException e) {
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
            return null;
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

        String name = className;
        while (found == null && name != null) {
            found = unObfToObfMappings.get(name + "/" + methodName + " " + methodArgs);
            name = inheritanceTree.get(name);
        }

        if (found == null) {
            handleUnmapped(remap.METHOD_NAME, className + "/" + methodName + " " + methodArgs);
            return null;
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

        String name = className;
        while (found == null && name != null) {
            found = unObfToObfMappings.get(name + "/" + fieldName);
            name = inheritanceTree.get(name);
        }

        if (found == null) {
            handleUnmapped(remap.FIELD_ACCESS, className + "/" + fieldName);
            return null;
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

        String name = className;
        while (found == null && name != null) {
            found = unObfToObfMappings.get(name + "/" + methodName + " " + methodSignature);
            name = inheritanceTree.get(name);
        }

        if (found == null) {
            handleUnmapped(remap.METHOD_ACCESS, className + "/" + methodName + " " + methodSignature);
            return null;
        }

        return found;
    }

}
