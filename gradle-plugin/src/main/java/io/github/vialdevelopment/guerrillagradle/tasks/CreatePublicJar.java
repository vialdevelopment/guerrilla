package io.github.vialdevelopment.guerrillagradle.tasks;

import io.github.vialdevelopment.guerrillagradle.Mapper;
import io.github.vialdevelopment.guerrillagradle.util.MiscUtil;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.JavaCompile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
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
        File publicsListFile = new File(getProject().getBuildDir() + "/tmp/guerrilla/publicsList");
        File publicsJarFile = new File(getProject().getBuildDir() + "/tmp/guerrilla/public.jar");
        File inheritanceTreeFile = new File(getProject().getBuildDir() + "/tmp/guerrilla/tree");

        try {
            List<String> cached = null;
            if (publicsListFile.exists()) {
                FileInputStream fileInputStream = new FileInputStream(publicsListFile);
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                cached = (List<String>) objectInputStream.readObject();
                objectInputStream.close();
                fileInputStream.close();
            }
            if ((cached != null && !cached.equals(makePublics)) || !publicsJarFile.exists() || !publicsListFile.exists() || !inheritanceTreeFile.exists()) {
                generate(publicsJarFile, publicsListFile, inheritanceTreeFile);
            } else {
                FileInputStream fileInputStream = new FileInputStream(inheritanceTreeFile);
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                inheritanceMap = (Map<String, String>) objectInputStream.readObject();
                objectInputStream.close();
                fileInputStream.close();
            }
            mapper.inheritanceTree = inheritanceMap;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    private void generate(File publicsJarFile, File publicsListFile, File inheritanceTreeFile) {
        try {
            publicsJarFile.getParentFile().mkdirs();
            ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(publicsJarFile, false));

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

                    if (jarEntry.getName().endsWith(".class")) {
                        ClassNode classNode = new ClassNode();
                        ClassReader classReader = new ClassReader(read(jarFile.getInputStream(jarEntry), jarEntry.getSize()));

                        final boolean[] transform = {false};
                        classReader.accept(new ClassVisitor(Opcodes.ASM9) {
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
                                return MiscUtil.toPublicName(internalName);
                            }
                        });
                        classReader.accept(classRemapper, 0);

                        boolean makePublic = false;
                        {
                            String externalName = MiscUtil.toNormalName(classNode.name);
                            for (String aPublic : makePublics) {
                                if (externalName.matches(aPublic)) {
                                    makePublic = true;
                                    break;
                                }
                            }
                        }
                        if (!makePublic) continue;

                        inheritanceMap.put(MiscUtil.toNormalName(classNode.name), MiscUtil.toNormalName(classNode.superName));

                        // make everything public
                        for (MethodNode method : classNode.methods) {
                            method.access = (method.access & (~ACC_PRIVATE)) | ACC_PUBLIC;
                            method.access = (method.access & (~ACC_PROTECTED)) | ACC_PUBLIC;
                        }
                        for (FieldNode field : classNode.fields) {
                            field.access = (field.access & (~ACC_PRIVATE)) | ACC_PUBLIC;
                            field.access = (field.access & (~ACC_PROTECTED)) | ACC_PUBLIC;
                            field.access = (field.access & (~ACC_FINAL));
                        }

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

        // cache make publics list
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(publicsListFile);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(makePublics);
            objectOutputStream.close();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // cache inheritance tree
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

    private byte[] read(InputStream inputStream, long size) throws IOException {
        byte[] bytes = new byte[(int) size];
        new DataInputStream(inputStream).readFully(bytes);
        inputStream.close();
        return bytes;
    }

}
