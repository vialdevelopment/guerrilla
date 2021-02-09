package io.github.vialdevelopment.guerrillagradle;

import org.gradle.api.Project;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * The Mapper is used to load mappings and called to obtain obfuscated names
 */
public class Mapper {
    /** The folder containing the mcp mappings */
    public Path mcpFolder;
    /** A hashmap containing the mappings from unObf -> obf */
    public Map<String, String> unObfToObfMappings = new HashMap<>();
    /** A hashmap of the inheritance structure of the minecraft classes */
    public Map<String, String> inheritanceMap;

    /**
     * Initializes the mapper to be ready to query
     * @param project gradle project
     * @param mcpVersion mcp version
     */
    public void init(Project project, String mcpVersion, String mappingsSRGFile) {
        if (mcpVersion.equals("") && mappingsSRGFile.equals("")) { // make sure mcp version is set
            System.out.println("MAPPINGS NOT SET");
            return;
        }

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

        if (mappingsSRGFile.equals("")) {
            // get the folder containing the mcp mappings
            String mcpType = "mcp_" + mcpVersion.split("_")[0];
            String mcpVersionNumber = mcpVersion.split("_")[1];
            mcpFolder = Paths.get(Paths.get(project.getGradle().getGradleUserHomeDir().getPath(), "caches/minecraft/de/oceanlabs/mcp/", mcpType + "/" + mcpVersionNumber).toString());
            // load the mappings
            loadMappingsFromFGCache();
        } else {
            try {
                parseSRGMappingsFile(new File(mappingsSRGFile));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(mappingsFile);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(unObfToObfMappings);
            objectOutputStream.close();
            fileOutputStream.close();

        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Loads the mappings,
     */
    public void loadMappingsFromFGCache() {
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
            parseSRGMappingsFile(mcpToNotchSrgFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void parseSRGMappingsFile(File mappingsFile) throws IOException {
        // read in the mappings
        FileInputStream inputStream = new FileInputStream(mappingsFile);
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
     * @param methodDesc arguments of method
     * @return obf name
     */
    public String remapMethodName(String className, String methodName, String methodDesc) {
        if (!methodName.equals("<init>")) {
            className = className.replace('.', '/');
            String found = unObfToObfMappings.get(className + "/" + methodName + " " + methodDesc);

            String name = className;
            while (found == null && name != null) {
                found = unObfToObfMappings.get(name + "/" + methodName + " " + methodDesc);
                name = inheritanceMap.get(name);
            }

            if (found == null) {
                handleUnmapped(remap.METHOD_NAME, className + "/" + methodName + " " + methodDesc);
                return null;
            }

            String[] wholeSplit = found.split(" ");

            return wholeSplit[0].substring(wholeSplit[0].lastIndexOf("/")+1) + " " + wholeSplit[1];
        } else {

            StringBuilder parameters = new StringBuilder();

            boolean inName = false;
            StringBuilder current = new StringBuilder();
            for (char c : methodDesc.toCharArray()) {
                if (c == '(') {
                } else if (c == 'L') {
                    inName = true;
                } else if (c == ';') {
                    inName = false;
                    String remapped = remapClassName(current.toString());
                    if (remapped != null) {
                        parameters.append("L").append(remapped).append(";");
                    } else {
                        parameters.append('L').append(current).append(';');
                    }
                    current = new StringBuilder();

                } else if (!inName) {
                    if (c == ')') { // end of parameters
                        break;
                    } else if (c != '(') {
                        parameters.append(c);
                    } else {
                        current.append(c);
                    }
                } else {
                    current.append(c);
                }
            }
            return  "<init> (" + parameters + ")" + methodDesc.charAt(methodDesc.length()-1);
        }
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
            name = inheritanceMap.get(name);
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
            name = inheritanceMap.get(name);
        }

        if (found == null) {
            handleUnmapped(remap.METHOD_ACCESS, className + "/" + methodName + " " + methodSignature);
            return null;
        }

        return found;
    }

}
