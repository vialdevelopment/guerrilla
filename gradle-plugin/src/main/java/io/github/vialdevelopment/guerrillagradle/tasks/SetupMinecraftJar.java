package io.github.vialdevelopment.guerrillagradle.tasks;

import io.github.vialdevelopment.guerrillagradle.Mapper;
import io.github.vialdevelopment.guerrillagradle.util.MiscUtil;
import io.github.vialdevelopment.guerrillagradle.util.NameTreeSet;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.objectweb.asm.Opcodes.*;

public class SetupMinecraftJar extends DefaultTask {
    /** mapper instance */
    public Mapper mapper;
    /** forge version */
    public String forgeVersion;
    /** mcp version */
    public String mcpVersion;
    /** class inheritance tree */
    public NameTreeSet inheritanceTree = new NameTreeSet("", null);

    /**
     * This sets up the minecraft public jar and inheritance tree and caches them
     */
    @TaskAction
    public void process() {
        try {
            // get locations of minecraft jar and caches
            JarFile minecraftJarFile = new JarFile(Paths.get(getProject().getGradle().getGradleUserHomeDir().getPath(),
                    "caches/minecraft/net/minecraftforge/forge", forgeVersion,
                    mcpVersion.split("_")[0],
                    mcpVersion.split("_")[1], "forgeBin-" + forgeVersion + ".jar").toString());

            // this is ugly
            String inheritanceTreeCacheLocation = getProject().getGradle().getGradleUserHomeDir().getPath() + "/caches/minecraft/guerrilla/io/github/vialdevelopment/minecraft-public/" + mcpVersion + forgeVersion + "/minecraft-public.tree";
            String minecraftPublicJarLocation = getProject().getGradle().getGradleUserHomeDir().getPath() + "/caches/minecraft/guerrilla/io/github/vialdevelopment/minecraft-public/" + mcpVersion + forgeVersion + "/minecraft-public.jar";

            File minecraftPublicJarFile = new File(minecraftPublicJarLocation);
            File inheritanceTreeCacheFile = new File(inheritanceTreeCacheLocation);
            // check if both the minecraft-public jar and inheritance tree files exist
            if (!(minecraftPublicJarFile.exists() && inheritanceTreeCacheFile.exists())) {
                minecraftPublicJarFile.getParentFile().mkdirs();
                ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(minecraftPublicJarFile, false));

                Enumeration<JarEntry> entries = minecraftJarFile.entries();
                // loop over all classes in minecraft jar
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().endsWith(".class")) {
                        if (entry.getName().startsWith("net/minecraftforge")) continue; // don't remap net/minecraftforge
                        // read in class
                        ClassNode classNode = new ClassNode();
                        ClassReader classReader = new ClassReader(read(minecraftJarFile.getInputStream(entry), entry.getSize()));
                        // everything goes to new naming convention
                        ClassVisitor classRemapper = new ClassRemapper(classNode, new Remapper() {
                            @Override
                            public String map(String internalName) {
                                if (internalName.startsWith("net/minecraft/")) {
                                    return MiscUtil.toPublicMinecraft(internalName);
                                }
                                return internalName;
                            }
                        });
                        classReader.accept(classRemapper, ClassReader.EXPAND_FRAMES);

                        // add this class to the inheritance tree
                        inheritanceTree.add(MiscUtil.toNormalMinecraft(classNode.superName), MiscUtil.toNormalMinecraft(classNode.name));

                        // make everything public, needed so that hooks have access to everything
                        for (MethodNode method : classNode.methods) {
                            method.access = (method.access & (~ACC_PRIVATE)) | ACC_PUBLIC;
                            method.access = (method.access & (~ACC_PROTECTED)) | ACC_PUBLIC;
                        }
                        for (FieldNode field : classNode.fields) {
                            field.access = (field.access & (~ACC_PRIVATE)) | ACC_PUBLIC;
                            field.access = (field.access & (~ACC_PROTECTED)) | ACC_PUBLIC;
                            field.access = (field.access & (~ACC_FINAL));
                        }
                        // write this class
                        ClassWriter classWriter = new ClassWriter(0);
                        classNode.accept(classWriter);
                        byte[] bytes = classWriter.toByteArray();

                        zipOutputStream.putNextEntry(new ZipEntry(classNode.name + ".class"));
                        zipOutputStream.write(bytes);
                    }
                }

                zipOutputStream.close();
                minecraftJarFile.close();

                mapper.inheritanceTree = inheritanceTree;

                // cache the inheritance tree
                FileOutputStream inheritanceTreeFileOut = new FileOutputStream(inheritanceTreeCacheLocation);
                ObjectOutputStream inheritanceTreeObjectOut = new ObjectOutputStream(inheritanceTreeFileOut);
                inheritanceTreeObjectOut.writeObject(inheritanceTree);
                inheritanceTreeObjectOut.close();
                inheritanceTreeFileOut.close();

            } else {
                // read in the inheritance tree
                FileInputStream inheritanceTreeFileIn = new FileInputStream(inheritanceTreeCacheLocation);
                ObjectInputStream inheritanceTreeObjectIn = new ObjectInputStream(inheritanceTreeFileIn);
                inheritanceTree = (NameTreeSet) inheritanceTreeObjectIn.readObject();
                mapper.inheritanceTree = inheritanceTree;
                inheritanceTreeObjectIn.close();
                inheritanceTreeFileIn.close();

            }

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    private byte[] read(InputStream inputStream, long size) throws IOException {
        byte[] bytes = new byte[(int) size];
        new DataInputStream(inputStream).readFully(bytes);
        return bytes;
    }

}
