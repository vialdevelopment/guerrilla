package io.github.vialdevelopment.guerrillagradle.tasks;

import io.github.vialdevelopment.guerrillagradle.util.MiscUtil;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Fixes all the classes to be usable at runtime
 */
public class FixAllClasses extends DefaultTask {

    /** project build directory */
    public File buildClassesDirectory;
    /** project build resources folder */
    public File resourcesDir;
    /** make publics list */
    public List<String> makePublics;
    /** transformers package */
    public String transformers;
    /** main transformer class */
    public String transformer;
    /** classes being transformed */
    public TreeSet<String> alreadyUsedTransformers;
    /** classes transformers are transforming */
    public Map<String, String> transformersTransforming;

    @TaskAction
    public void transform() {
        // go over all classes to get references to PUBLICs abuse
        TreeSet<String> publicsUsed = new TreeSet<>(); // a list of all public classes referenced
        try {
            Files.walk(buildClassesDirectory.toPath()).forEach(path -> {
                try {
                    // make sure is file and isn't one we generated
                    if (path.toFile().isFile() && path.endsWith(".class")) {
                        // read in the class file
                        byte[] transformerBytes = Files.readAllBytes(path);

                        ClassNode classNode = new ClassNode();
                        ClassReader classReader = new ClassReader(transformerBytes);

                        // remap publics used
                        ClassVisitor classRemapper;

                        if (classNode.name.equals(transformer)) {
                            classRemapper = new ClassRemapper(classNode, new Remapper() {
                                @Override
                                public String map(String internalName) {
                                    if (MiscUtil.isPublicName(internalName, makePublics)) {
                                        String normalizedName = MiscUtil.toNormalName(internalName, makePublics);
                                        publicsUsed.add(normalizedName);
                                        return normalizedName;
                                    }
                                    return internalName;
                                }
                            });
                        } else {
                            classRemapper = new ClassRemapper(classNode, new Remapper() {
                                @Override
                                public String map(String internalName) {
                                    if (MiscUtil.isPublicName(internalName, makePublics)) {
                                        String normalizedName = MiscUtil.toNormalName(internalName, makePublics);
                                        publicsUsed.add(normalizedName);
                                        return normalizedName;
                                    }
                                    if (internalName.startsWith(transformers.replace('.', '/'))) {
                                        System.out.println(internalName);
                                        System.out.println(transformersTransforming);
                                        return transformersTransforming.get(internalName);
                                    }
                                    return internalName;
                                }
                            });
                        }

                        classReader.accept(classRemapper, 0);


                        // write the fixed class back
                        ClassWriter classWriter = new ClassWriter(0);
                        classNode.accept(classWriter);
                        Files.write(path, classWriter.toByteArray());
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            // now we write the classes to receive the public and non-final abuse to a file
            // to be done at runtime
            // FIXME this shouldn't be always in the main submodule
            String makePublicTXTPath = resourcesDir + "/main/guerrilla-make-public.txt";
            File makePublicTXTFile = new File(makePublicTXTPath);
            makePublicTXTFile.getParentFile().mkdirs();
            FileWriter fileWriter = new FileWriter(makePublicTXTFile);
            publicsUsed.removeIf(alreadyUsedTransformers::contains);
            for (String s : publicsUsed) {
                fileWriter.write(s);
                fileWriter.write("\n");
            }
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
