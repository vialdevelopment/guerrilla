package io.github.vialdevelopment.guerrillagradle.tasks;

import io.github.vialdevelopment.guerrillagradle.GuerrillaGradlePluginExtension;
import io.github.vialdevelopment.guerrillagradle.Mapper;
import io.github.vialdevelopment.guerrillagradle.util.NameUtil;
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
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import static org.objectweb.asm.Opcodes.ASM5;

/**
 * Fixes all the classes to be usable at runtime
 */
public class FixAllClassesTask extends DefaultTask {
    /** config extension */
    public GuerrillaGradlePluginExtension extension;
    /** project build directory */
    public File buildClassesDirectory;
    /** project build resources folder */
    public File resourcesDir;
    /** classes being transformed */
    public TreeSet<String> alreadyUsedTransformers;
    /** classes transformers are transforming */
    public Map<String, String> transformersTransforming;
    /** mapper */
    public Mapper mapper;

    @TaskAction
    public void transform() {
        // go over all classes to get references to PUBLICs abuse
        TreeSet<String> publicsUsed = new TreeSet<>(); // a list of all public classes referenced
        try {
            Files.walk(buildClassesDirectory.toPath()).forEach(path -> {
                try {
                    // make sure is file and isn't one we generated
                    if (path.toFile().isFile() && path.toString().endsWith(".class") && !path.toString().equals("module-info.class")) {
                        // read in the class file
                        byte[] transformerBytes = Files.readAllBytes(path);

                        ClassNode classNode = new ClassNode();
                        ClassReader classReader = new ClassReader(transformerBytes);


                        final boolean[] isMainTransformer = {false};

                        classReader.accept(new ClassVisitor(ASM5) {
                            @Override
                            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                                if (name.startsWith(extension.transformersPackage) || name.equals(extension.transformerRegistrationClass)) {
                                    isMainTransformer[0] = true;
                                }
                            }
                        }, 0);

                        // remap publics used
                        ClassVisitor classRemapper;
                        if (isMainTransformer[0]) {
                            classRemapper = new ClassRemapper(classNode, new Remapper() {
                                @Override
                                public String map(String internalName) {
                                    if (NameUtil.isPublicName(internalName, extension.makePublic)) {
                                        String normalizedName = NameUtil.toNormalName(internalName, extension.makePublic);
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
                                    if (NameUtil.isPublicName(internalName, extension.makePublic)) {
                                        String normalizedName = NameUtil.toNormalName(internalName, extension.makePublic);
                                        publicsUsed.add(normalizedName);
                                        return normalizedName;
                                    }
                                    if (internalName.startsWith(extension.transformersPackage)) {
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
            String makePublicTXTPath = resourcesDir + "/main/guerrilla-make-public-unobf.txt";
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
        if (extension.remap) {
            try {
                // now we write the classes to receive the public and non-final abuse to a file
                // to be done at runtime
                // FIXME this shouldn't be always in the main submodule
                // remap names for obf
                TreeSet<String> remappedPublicsUsed = new TreeSet<>();
                for (Iterator<String> iterator = publicsUsed.iterator(); iterator.hasNext(); ) {
                    String s = iterator.next();
                    String remapped = mapper.remapClassName(s);
                    remappedPublicsUsed.add(remapped != null ? remapped : s);
                }
                String makePublicTXTPath = resourcesDir + "/main/guerrilla-make-public-obf.txt";
                File makePublicTXTFile = new File(makePublicTXTPath);
                makePublicTXTFile.getParentFile().mkdirs();
                FileWriter fileWriter = new FileWriter(makePublicTXTFile);
                for (String s : remappedPublicsUsed) {
                    fileWriter.write(s);
                    fileWriter.write("\n");
                }
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
