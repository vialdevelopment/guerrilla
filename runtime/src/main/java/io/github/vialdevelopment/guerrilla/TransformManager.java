package io.github.vialdevelopment.guerrilla;

import io.github.vialdevelopment.guerrilla.annotation.TransformClass;
import io.github.vialdevelopment.guerrilla.asm.CheckClassAdapterClassNode;
import io.github.vialdevelopment.guerrilla.transform.ITransform;
import io.github.vialdevelopment.guerrilla.transform.TransformAddInterfacesEx;
import io.github.vialdevelopment.guerrilla.transform.TransformInlineFieldsEx;
import io.github.vialdevelopment.guerrilla.transform.TransformMethodEx;
import net.minecraft.launchwrapper.Launch;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * A manager for applying and organizing the Transforms
 *
 * @author cats
 * @since October 4, 2020
 */
public class TransformManager {

    public static boolean HAS_INIT = false;

    public static boolean OBF = false;

    public static boolean COMPUTE_FRAMES = true;

    public static ClassLoader classLoader = TransformManager.class.getClassLoader();

    public static boolean ASM_DEBUG = Boolean.parseBoolean(System.getProperty("guerrilla.asmdebug", "false"));

    private static Map<String, List<String>> transformMap = new HashMap<>();

    private static final Map<String, byte[]> untransformedClassesBytesCache = new HashMap<>();

    private static List<ITransform> transforms = new ArrayList<>();

    private static List<String> transformExclude = new ArrayList<>();

    public static void init() {
        transformMap = new HashMap<>();
        transformExclude = new ArrayList<>();
        transforms = new ArrayList<>();
        try {
            Class.forName("net.minecraft.launchwrapper.Launch", false, TransformManager.class.getClassLoader());
            if (Launch.blackboard != null) {
                OBF = !(Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");
                classLoader = Launch.classLoader;
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        // read in all guerrilla properties
        try {
            Enumeration<URL> enumeration = TransformManager.class.getClassLoader().getResources("guerrilla.properties");
            while (enumeration.hasMoreElements()) {
                URL currentURL = enumeration.nextElement();
                Properties properties = new Properties();
                properties.load(currentURL.openStream());
                // check if compute frames
                if (COMPUTE_FRAMES) {
                    if (properties.getProperty("compute-frames").equals("false")) COMPUTE_FRAMES = false;
                }
                // load make publics
                String makePublics = properties.getProperty(OBF ? "make-public-obf" : "make-public-unobf");
                for (String s : makePublics.split(";")) {
                    List<String> names = new ArrayList<>();
                    names.add("");
                    transformMap.put(s.replace('/', '.'), names);
                }
                // load transform excludes
                String transformExcludes = properties.getProperty("transform-exclude");
                for (String s : transformExcludes.split(";")) {
                    transformExclude.add(s.replace('/', '.'));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        transforms.add(new TransformAddInterfacesEx());
        transforms.add(new TransformMethodEx());
        transforms.add(new TransformInlineFieldsEx());

        HAS_INIT = true;
    }

    /**
     * @param transform the {@link Object} to add
     *
     * Adds them to the list and sets some internal bits that make it easier to do stuff
     */
    public static void addTransform(Class transform) {

        final TransformClass transformClassAnnotation = (TransformClass) transform.getAnnotation(TransformClass.class);

        if (transformClassAnnotation == null) return;

        final String className = OBF ? transformClassAnnotation.obfName() : transformClassAnnotation.name();

        if (transformMap.containsKey(className)) {
            transformMap.get(className).add(transform.getName());
        } else {
            List<String> names = new ArrayList<>();
            names.add(transform.getName());
            transformMap.put(className, names);
        }
        transformExclude.add(transform.getName());
    }

    /**
     * This is the main class transformer point.<br>
     * First it is checked if the class is excluded from transformation, and if not,<br>
     * Then:<br>
     *  - Parse the class using ASM<br>
     *  - Run transformers through it<br>
     *  - Make the transformed class completely public<br>
     *  - Remaps references from the transformer to the class being transformed<br>
     *  - Class is written<br>
     *  - if {@link TransformManager#ASM_DEBUG} then dump the class<br>
     *
     * @param name the class name
     * @param basicClass the class
     * @return the byte array of the transformed class
     */
    public static byte[] transformClass(String name, byte[] basicClass) {
        // if we're excluding this class we probably use it elsewhere here and so want it cached
        if (transformExclude.contains(name)) {
            untransformedClassesBytesCache.put(name, basicClass);
            return basicClass;
        }

        final List<String> transformers = transformMap.get(name);

        if (transformers == null) return basicClass;

        System.out.println("Transforming " + name);

        ClassNode classNodeBeingTransformed = new ClassNode();
        ClassReader classReader = new ClassReader(basicClass);
        classReader.accept(classNodeBeingTransformed, 0);

        File dumpFile = new File("Guerrilla" + File.separator + "asm/" + ASMUtil.toExternalName(name) + ".class");

        try {
            for (String transform : transformers) {
                if (!transform.equals("")) {

                    // get transformer class ASM
                    ClassNode transformerNode = new ClassNode();
                    ClassReader transformerReader = new ClassReader(untransformedClassesBytesCache.get(transform));
                    transformerReader.accept(transformerNode, 0);

                    for (ITransform iTransform : transforms) {
                        iTransform.transform(classNodeBeingTransformed, transformerNode);
                    }
                }
            }

            // make everything public and fields non-final, needed so that hooks have access to everything
            // this is done regardless if there was a transformer class for this class
            ASMUtil.makeClassPublic(classNodeBeingTransformed);

            List<String> externalTransformersNames = new ArrayList<>();
            for (String transformer : transformers) {
                if (transformer.equals("")) continue;
                externalTransformersNames.add(ASMUtil.toExternalName(transformer));
            }
            int flags;
            if (COMPUTE_FRAMES) {
                flags = ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS;
            } else {
                classNodeBeingTransformed.methods.forEach(methodNode -> methodNode.instructions.forEach(abstractInsnNode -> { if (abstractInsnNode instanceof FrameNode) { methodNode.instructions.remove(abstractInsnNode);}}));
                flags = 0;
            }
            ClassWriter classWriter = new ClassWriter(flags) {
                // we override this to make it use our class loader instead of what the ClassWriter class was loaded from
                @Override
                protected String getCommonSuperClass(String type1, String type2) {
                    Class<?> class1;
                    try {
                        class1 = classLoader.loadClass(type1.replace('/', '.'));
                    } catch (ClassNotFoundException e) {
                        throw new TypeNotPresentException(type1, e);
                    }
                    Class<?> class2;
                    try {
                        class2 = classLoader.loadClass(type2.replace('/', '.'));
                    } catch (ClassNotFoundException e) {
                        throw new TypeNotPresentException(type2, e);
                    }
                    if (class1.isAssignableFrom(class2)) {
                        return type1;
                    }
                    if (class2.isAssignableFrom(class1)) {
                        return type2;
                    }
                    if (class1.isInterface() || class2.isInterface()) {
                        return "java/lang/Object";
                    } else {
                        do {
                            class1 = class1.getSuperclass();
                        } while (!class1.isAssignableFrom(class2));
                        return class1.getName().replace('.', '/');
                    }
                }
            };

            // remap all references from the transformer to class being transformed
            ClassVisitor classRemapper = new ClassRemapper(classWriter, new Remapper() {
                @Override
                public String map(String internalName) {
                    if (externalTransformersNames.contains(internalName)) {
                        return ASMUtil.toExternalName(name);
                    }
                    return internalName;
                }
            });

            classNodeBeingTransformed.accept(classRemapper);
            byte[] classBytes = classWriter.toByteArray();

            if (ASM_DEBUG) {
                dumpDebug(dumpFile, classNodeBeingTransformed, classBytes);
            }

            return classBytes;
        } catch (Exception e) {
            e.printStackTrace();
            dumpDebug(dumpFile, classNodeBeingTransformed);
        }
        return basicClass;
    }



    public static void dumpDebug(File dumpFile, ClassNode classNode) {
        File traceDumpFile = new File(dumpFile.getPath() + ".trace.dump");
        File verifyDumpFile = new File(dumpFile.getPath() + ".verify.dump");
        traceDumpFile.getParentFile().mkdirs();
        FileWriter traceFileWriter;
        FileWriter verifyFileWriter;
        try {
            traceFileWriter = new FileWriter(traceDumpFile);
            verifyFileWriter = new FileWriter(verifyDumpFile);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        PrintWriter printDumpFileLog = new PrintWriter(traceFileWriter);
        PrintWriter verifyDumpFileLog = new PrintWriter(verifyFileWriter);

        try {
            // write disassembly output to log file
            TraceClassVisitor traceClassVisitor = new TraceClassVisitor(printDumpFileLog);
            classNode.accept(traceClassVisitor);
        } catch (Exception e) {
            e.printStackTrace();
        }

        StringWriter sw = new StringWriter();

        // write verification output to log file
        try {
            CheckClassAdapterClassNode.verify(classNode, true, verifyDumpFileLog);
            printDumpFileLog.println(sw.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            traceFileWriter.close();
            printDumpFileLog.close();
            sw.close();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    public static void dumpDebug(File dumpFile, ClassNode classNode, byte[] classBytes) {

        dumpDebug(dumpFile, classNode);

        File disassembleDumpFile = new File(dumpFile.getPath() + ".diss.dump");
        disassembleDumpFile.getParentFile().mkdirs();
        FileWriter disassembleFileWriter;
        try {
            disassembleFileWriter = new FileWriter(disassembleDumpFile);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        PrintWriter disassembleDumpFileLog = new PrintWriter(disassembleFileWriter);

        try {
            // dump class to file
            new FileOutputStream(dumpFile).write(classBytes);
        } catch (IOException fileNotFoundException) {
            fileNotFoundException.printStackTrace();
        }

        StringWriter sw = new StringWriter();

        try {
            Process process = Runtime.getRuntime().exec(new String[]{"javap", "-c", "-p", dumpFile.toString()});
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String s;
            while ((s = bufferedReader.readLine()) != null) {
                disassembleDumpFileLog.println(s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            disassembleDumpFileLog.close();
            sw.close();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

}
