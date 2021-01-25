package io.github.vialdevelopment.guerrilla;

import io.github.vialdevelopment.guerrilla.annotation.TransformClass;
import io.github.vialdevelopment.guerrilla.asm.CheckClassAdapterClassNode;
import io.github.vialdevelopment.guerrilla.transform.*;
import net.minecraft.launchwrapper.Launch;
import org.mutabilitydetector.asm.NonClassloadingClassWriter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.*;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.*;

import static org.objectweb.asm.Opcodes.*;

/**
 * A manager for applying and organizing the Transforms
 *
 * @author cats
 * @since October 4, 2020
 */
public class TransformManager {

    public static boolean HAS_INIT = false;

    public static boolean OBF = false;

    public static boolean ASM_DEBUG = Boolean.parseBoolean(System.getProperty("guerrilla.asmdebug", "false"));

    private static final Map<String, List<String>> transformMap = new HashMap<>();

    private static final Map<String, byte[]> untransformedClassesBytesCache = new HashMap<>();

    private static final List<ITransform> transforms = new ArrayList<>();

    private static final List<String> transformExclude = new ArrayList<>();

    public static void init() {
        if (HAS_INIT) return;
        try {
            Class.forName("net.minecraft.launchwrapper.Launch");
            if (Launch.blackboard != null) {
                OBF = !(Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        try {
            // read in all guerrilla-make-public.txt files
            Enumeration<URL> enumeration = TransformManager.class.getClassLoader().getResources(OBF ? "guerrilla-make-public-obf.txt" : "guerrilla-make-public-unobf.txt");
            while (enumeration.hasMoreElements()) {
                URL currentURL = enumeration.nextElement();
                try {
                    InputStream inputStream = currentURL.openStream();
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    BufferedReader reader = new BufferedReader(inputStreamReader);
                    String current;

                    while ((current = reader.readLine()) != null) {
                        List<String> names = new ArrayList<>();
                        names.add("");
                        transformMap.put(current.replace('/', '.'), names);
                    }

                    reader.close();
                    inputStreamReader.close();
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // read in all guerrilla-transform-exclude.txt files
        try {
            Enumeration<URL> enumeration = TransformManager.class.getClassLoader().getResources("guerrilla-transform-exclude.txt");
            while (enumeration.hasMoreElements()) {
                URL currentURL = enumeration.nextElement();
                try {
                    InputStream inputStream = currentURL.openStream();
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    BufferedReader reader = new BufferedReader(inputStreamReader);
                    String current;

                    while ((current = reader.readLine()) != null) {
                        transformExclude.add(current.replace('/', '.'));
                    }

                    reader.close();
                    inputStreamReader.close();
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        transforms.add(new TransformAddInterfacesEx());
        transforms.add(new TransformsFieldAccessEx());
        transforms.add(new TransformMethodEx());

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

        final String className = OBF ? transformClassAnnotation.obfClassName() : transformClassAnnotation.className();

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
     * invoke this in your Class transformer's transform method
     *
     * First it is checked if there's a transformer for this class, if not, just return the original
     *
     * Now the class is read with asm, and the transformer class is also fetched in asm to avoid reflection
     *
     * All interfaces of the transformer are added to the class
     *
     * Then field access modifiers are processed, by looping over the fields,
     *  checking if they have annotations, check if one of those annotations is the field access transformer,
     *  then if so reads the annotation into a new field access transformer object.
     *  Then we check if one of the class field nodes matches the annotation, if so its access is modified
     *
     * If there is no annotation on the field, it is added to the class
     *
     * Now the method transformers are processed. The methods are looped over, checking their annotations
     *  if they don't have the transformMethod annotation they are inlined as long as they aren't . Otherwise
     *  the methods are looped over, checking if they match the annotation, and if so are transformed.
     *
     * For insertions, first the init for call back is removed, returns are removed, and callBack.cancel() calls
     *  are changed into actual returns
     *
     * Field references are now fixed, to enable ducks, eg reference to duck will go to the class instead
     *
     * Finally, if we have the java arg ASM_DEBUG true, we dump the transformed class to the Canopy/asm folder
     *   and the transformed class is returned
     *
     * @param name the class name
     * @param basicClass the basic class
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

            for (MethodNode method : classNodeBeingTransformed.methods) {
                method.access = (method.access & (~ACC_PRIVATE)) | ACC_PUBLIC;
                method.access = (method.access & (~ACC_PROTECTED)) | ACC_PUBLIC;
            }
            for (FieldNode field : classNodeBeingTransformed.fields) {
                field.access = (field.access & (~ACC_PRIVATE)) | ACC_PUBLIC;
                field.access = (field.access & (~ACC_PROTECTED)) | ACC_PUBLIC;
                field.access = (field.access & (~ACC_FINAL));
            }

            // only need to fix references if there was a transformer
            List<String> externalTransformersNames = new ArrayList<>();
            for (String transformer : transformers) {
                if (transformer.equals("")) continue;
                externalTransformersNames.add(ASMUtil.toExternalName(transformer));
            }
            if (externalTransformersNames.size() != 0) {
                String classBeingTransformedExternalName = ASMUtil.toExternalName(name);
                // remap all references from the transformer to class being transformed
                ClassNode temp = new ClassNode();
                ClassVisitor classRemapper = new ClassRemapper(temp, new Remapper() {
                    @Override
                    public String map(String internalName) {
                        if (externalTransformersNames.contains(internalName)) {
                            return classBeingTransformedExternalName;
                        }
                        return internalName;
                    }
                });
                classNodeBeingTransformed.accept(classRemapper);
                classNodeBeingTransformed = temp;
            }

            if (ASM_DEBUG) {
                dumpDebug(dumpFile, classNodeBeingTransformed);
            }

            NonClassloadingClassWriter classWriter = new NonClassloadingClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classNodeBeingTransformed.accept(classWriter);

            return classWriter.toByteArray();
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
        verifyDumpFile.getParentFile().mkdirs();
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
            printDumpFileLog.println("########## Disassembly of Generated ##########");
            TraceClassVisitor traceClassVisitor = new TraceClassVisitor(printDumpFileLog);
            classNode.accept(traceClassVisitor);

        } catch (Exception e) {
            e.printStackTrace();
        }

        StringWriter sw = new StringWriter();
        // write verification output to log file
        try {
            verifyDumpFileLog.println("############### Class verifier ###############");

            CheckClassAdapterClassNode.verify(classNode, null, true, verifyDumpFileLog);

            printDumpFileLog.println(sw.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            NonClassloadingClassWriter classWriter = new NonClassloadingClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classNode.accept(classWriter);
            byte[] classBytes = classWriter.toByteArray();
            // dump class to file
            new FileOutputStream(dumpFile).write(classBytes);

        } catch (IOException fileNotFoundException) {
            fileNotFoundException.printStackTrace();
        }



        try {
            traceFileWriter.close();
            printDumpFileLog.close();
            sw.close();

        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

}
