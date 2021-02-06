package io.github.vialdevelopment.guerrillagradle.tasks;

import io.github.vialdevelopment.guerrillagradle.Mapper;
import io.github.vialdevelopment.guerrillagradle.util.MiscUtil;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.JavaCompile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.objectweb.asm.Opcodes.*;

public class CreatePublicJar extends DefaultTask {

    public JavaCompile javaCompile;
    public Mapper mapper;
    public List<String> makePublics;
    public Map<String, String> inheritanceMap = new HashMap<>();

    @TaskAction
    public void process() {
        File publicsJarFile = new File(getProject().getBuildDir() + "/tmp/guerrilla/public.jar");
        File inheritanceTreeFile = new File(getProject().getBuildDir() + "/tmp/guerrilla/tree");

        try {
            // if public jar and inheritance tree exist, read them in
            if (publicsJarFile.exists() && inheritanceTreeFile.exists()) {
                FileInputStream fileInputStream = new FileInputStream(inheritanceTreeFile);
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                inheritanceMap = (Map<String, String>) objectInputStream.readObject();
                objectInputStream.close();
                fileInputStream.close();

            } else {
                // otherwise, create them
                try {
                    publicsJarFile.getParentFile().mkdirs();
                    ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(publicsJarFile, false));
                    // loop over every jar javaCompile has to find the classes to public abuse
                    for (File file : javaCompile.getClasspath()) {
                        JarFile jarFile;
                        try {
                            jarFile = new JarFile(file);
                        } catch (Exception e) {
                            continue;
                        }

                        Enumeration<JarEntry> jarEntryEnumeration = jarFile.entries();
                        while (jarEntryEnumeration.hasMoreElements()) {
                            JarEntry jarEntry = jarEntryEnumeration.nextElement();

                            if (jarEntry.getName().endsWith(".class") && !jarEntry.getName().equals("module-info.class")) {

                                ClassNode classNode = new ClassNode();
                                ClassReader classReader = new ClassReader(readBytes(jarFile.getInputStream(jarEntry), jarEntry.getSize()));

                                // make sure class is one to public abuse
                                final boolean[] transform = {false};
                                classReader.accept(new ClassVisitor(ASM5) {
                                    @Override
                                    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                                        for (String aPublic : makePublics) {
                                            if (name.matches(aPublic)) {
                                                transform[0] = true;
                                                break;
                                            }
                                        }
                                    }
                                }, 0);
                                if (!transform[0]) continue;

                                // remap to new naming convention
                                ClassVisitor classRemapper = new ClassRemapper(classNode, new Remapper() {
                                    @Override
                                    public String map(String internalName) {
                                        return MiscUtil.toPublicName(internalName, makePublics);
                                    }
                                });

                                classReader.accept(classRemapper, 0);

                                inheritanceMap.put(MiscUtil.toNormalName(classNode.name, makePublics), MiscUtil.toNormalName(classNode.superName, makePublics));

                                // make everything public
                                classNode.access = (classNode.access & (~ACC_PRIVATE)) | ACC_PUBLIC;
                                classNode.access = (classNode.access & (~ACC_PROTECTED)) | ACC_PUBLIC;
                                classNode.access = classNode.access & (~ACC_FINAL);

                                classNode.innerClasses.forEach(innerClassNode -> {
                                    innerClassNode.access = (innerClassNode.access & (~ACC_PRIVATE)) | ACC_PUBLIC;
                                    innerClassNode.access = (innerClassNode.access & (~ACC_PROTECTED)) | ACC_PUBLIC;
                                    innerClassNode.access = innerClassNode.access & (~ACC_FINAL);
                                });

                                classNode.methods.forEach(methodNode -> {
                                    methodNode.access = (methodNode.access & (~ACC_PRIVATE)) | ACC_PUBLIC;
                                    methodNode.access = (methodNode.access & (~ACC_PROTECTED)) | ACC_PUBLIC;
                                    methodNode.access = methodNode.access & (~ACC_FINAL);
                                });
                                classNode.fields.forEach(fieldNode -> {
                                    fieldNode.access = (fieldNode.access & (~ACC_PRIVATE)) | ACC_PUBLIC;
                                    fieldNode.access = (fieldNode.access & (~ACC_PROTECTED)) | ACC_PUBLIC;
                                    fieldNode.access = fieldNode.access & (~ACC_FINAL);
                                });

                                ClassWriter classWriter = new ClassWriter(0);
                                classNode.accept(classWriter);
                                byte[] bytes = classWriter.toByteArray();

                                zipOutputStream.putNextEntry(new ZipEntry(classNode.name + ".class"));
                                zipOutputStream.write(bytes);
                            }
                        }
                    }

                    zipOutputStream.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }

                // cache inheritance map
                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(inheritanceTreeFile);
                    ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
                    objectOutputStream.writeObject(inheritanceMap);
                    objectOutputStream.close();
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            mapper.inheritanceMap = inheritanceMap;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    private byte[] readBytes(InputStream inputStream, long size) throws IOException {
        byte[] bytes = new byte[(int) size];
        new DataInputStream(inputStream).readFully(bytes);
        inputStream.close();
        return bytes;
    }

}
