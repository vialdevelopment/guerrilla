package io.github.vialdevelopment.guerrillagradle.mapping.mapper.api;

import io.github.vialdevelopment.guerrillagradle.mapping.mapper.api.name.ClassName;
import io.github.vialdevelopment.guerrillagradle.mapping.mapper.api.name.FieldName;
import io.github.vialdevelopment.guerrillagradle.mapping.mapper.api.name.MethodName;
import org.gradle.api.Project;

import java.io.*;
import java.util.Map;
import java.util.TreeMap;

/**
 * Mapper
 */
public abstract class Mapper {

    /** Class name mappings */
    protected Map<ClassName, ClassName> classNameMappings = new TreeMap<>();
    /** Method name mappings */
    protected Map<MethodName, MethodName> methodNameMappings = new TreeMap<>();
    /** Field name mappings */
    protected Map<FieldName, FieldName> fieldNameMappings = new TreeMap<>();
    /** A hashmap of the inheritance structure of the classes */
    protected Map<String, String> inheritanceMap;

    /**
     * Inits this mapper
     * @param project gradle project
     * @param mappings mappings
     */
    public abstract void init(Project project, String mappings);

    /**
     * Remaps this class name
     * @param className class name
     * @return remapped class name
     */
    public ClassName remapClassName(ClassName className) {
        ClassName remapped = classNameMappings.get(className);
        if (remapped != null) {
            return remapped;
        } else {
            System.out.println("Failed to remap " + className);
            return className;
        }
    }

    /**
     * Remaps this method name
     * @param methodName method name
     * @return remapped method name
     */
    public MethodName remapMethodName(MethodName methodName) {
        if (methodName.methodName.equals("<init>")) {
            ClassName remappedClassName = remapClassName(new ClassName(methodName.ownerName));
            if (remappedClassName == null) {
                remappedClassName = new ClassName(methodName.ownerName);
                System.out.println("Failed to remap " + remappedClassName);
            }
            return new MethodName(remappedClassName.className, "<init>", remapMethodDesc(methodName.methodDesc));
        } else {
            MethodName obfName = methodNameMappings.get(methodName);
            if (inheritanceMap != null) {
                String currentName = methodName.ownerName;
                MethodName temp = methodName.clone();
                while (obfName == null && currentName != null) {
                    currentName = inheritanceMap.get(currentName);
                    if (currentName == null) {
                        System.out.println("Failed to remap " + methodName);
                        break;
                    }
                    temp.ownerName = currentName;
                    obfName = methodNameMappings.get(temp);
                }
                if (obfName == null) {
                    obfName = methodName;
                    System.out.println("Failed to remap " + methodName);
                }
            }
            return obfName;
        }
    }

    /**
     * Remaps this field name
     * @param fieldName field name
     * @return remapped field name
     */
    public FieldName remapFieldName(FieldName fieldName) {
        FieldName obfName = fieldNameMappings.get(fieldName);
        if (obfName == null) {
            if (inheritanceMap != null) {
                String currentClass = fieldName.ownerName;
                FieldName temp = fieldName.clone();
                while (obfName == null && currentClass != null) {
                    temp.ownerName = currentClass;
                    obfName = fieldNameMappings.get(temp);
                    currentClass = inheritanceMap.get(currentClass);
                }
                if (obfName == null) {
                    System.out.println("Failed to remap " + fieldName);
                }
            }
        }
        return obfName;
    }

    /**
     * Remaps a field desc
     * @param unmappedFieldDesc field desc
     * @return remapped field desc
     */
    protected String remapFieldDesc(String unmappedFieldDesc) {
        if (unmappedFieldDesc.length() == 1) return unmappedFieldDesc;

        ClassName remapped = remapClassName(new ClassName(unmappedFieldDesc.substring(1, unmappedFieldDesc.length()-1)));
        return remapped == null ? unmappedFieldDesc : "L" + remapped.className + ";";
    }

    /**
     * Remaps a method desc
     * @param unmappedMethodDesc method desc
     * @return remapped method desc
     */
    protected String remapMethodDesc(String unmappedMethodDesc) {
        // method name
        StringBuilder parameters = new StringBuilder();

        boolean inName = false;
        StringBuilder current = new StringBuilder();
        for (char c : unmappedMethodDesc.toCharArray()) {
            if (c == '(') {
            } else if (c == 'L') {
                inName = true;
            } else if (c == ';') {
                inName = false;
                String remapped = remapClassName(new ClassName(current.toString())).className;
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
        String remappedMethodReturnType;
        if (unmappedMethodDesc.charAt(unmappedMethodDesc.length()-1) == ';') {
            String unmappedType = unmappedMethodDesc.substring(unmappedMethodDesc.lastIndexOf(")")+1, unmappedMethodDesc.length()-1);
            remappedMethodReturnType = classNameMappings.get(new ClassName(unmappedType)).className;
        } else {
            remappedMethodReturnType = unmappedMethodDesc.substring(unmappedMethodDesc.length()-1);
        }

        return  "(" + parameters + ")" + remappedMethodReturnType;
    }

    /**
     * Loads our cached mappings
     * @param project gradle project
     */
    public void loadFromCache(Project project) {
        File mappingsFile = new File(project.getBuildDir() + "/tmp/guerrilla/mappings");
        if (mappingsFile.exists()) {
            try {
                FileInputStream fileInputStream = new FileInputStream(mappingsFile);
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                loadFromCache(objectInputStream);
                objectInputStream.close();
                fileInputStream.close();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Loads our cached mappings objects
     * @param objectInputStream object input stream
     * @throws IOException Failure in reading the object
     * @throws ClassNotFoundException Class object read not found
     */
    public void loadFromCache(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        classNameMappings = (Map<ClassName, ClassName>) objectInputStream.readObject();
        methodNameMappings = (Map<MethodName, MethodName>) objectInputStream.readObject();
        fieldNameMappings = (Map<FieldName, FieldName>) objectInputStream.readObject();
    }

    /**
     * Caches our mappings
     * @param project gradle project
     */
    public void writeToCache(Project project) {
        File mappingsFile = new File(project.getBuildDir() + "/tmp/guerrilla/mappings");
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(mappingsFile);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            writeToCache(objectOutputStream);
            objectOutputStream.close();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Caches our mappings objects
     * @param objectOutputStream object output stream
     * @throws IOException failure writing objects
     */
    public void writeToCache(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.writeObject(classNameMappings);
        objectOutputStream.writeObject(methodNameMappings);
        objectOutputStream.writeObject(fieldNameMappings);
    }

    /**
     * Set the inheritance map
     * @param inheritanceMap inheritance map
     */
    public void setInheritanceMap(Map<String, String> inheritanceMap) {
        this.inheritanceMap = inheritanceMap;
    }

}
