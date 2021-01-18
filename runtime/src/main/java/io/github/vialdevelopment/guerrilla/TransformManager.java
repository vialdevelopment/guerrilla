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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

/**
 * A manager for applying and organizing the Transforms
 *
 * @author cats
 * @since October 4, 2020
 */
public class TransformManager {

    public static boolean HAS_INIT = false;

    public static boolean OBF = !(Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");

    public static boolean ASM_DEBUG = Boolean.parseBoolean(System.getProperty("guerrilla.asmdebug", "false"));

    private static final Map<String, String> transformMap = new HashMap<>();

    public static Map<String, byte[]> transformerClassesCache = new HashMap<>();

    private static final List<ITransform> transforms = new ArrayList<>();

    public static void init() {
        InputStream toPublicAbuseInStream = TransformManager.class.getClassLoader().getResourceAsStream("make-public.txt");
        if (toPublicAbuseInStream != null) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(toPublicAbuseInStream));
                String current;
                while ((current = reader.readLine()) != null) {
                    transformMap.put(current.replace('/', '.'), "");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
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

        getTransformMap().put(className, transform.getName());
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
        // FIXME remove specifics to canopy and make configurable somehow, maybe file in resources
        if (name.startsWith("io.github.vialdevelopment.canopy.asm.transformers")) {
            transformerClassesCache.put(name, basicClass);
        }

        if (name.startsWith("net.minecraftforge") || name.startsWith("io.github.vialdevelopment")) return basicClass;

        final String transform = getTransformMap().get(name);

        if (transform == null) return basicClass;

        System.out.println("Transforming " + name);

        ClassNode classNodeBeingTransformed = new ClassNode();
        ClassReader classReader = new ClassReader(basicClass);
        classReader.accept(classNodeBeingTransformed, 0);

        ClassNode transformerNode = new ClassNode();

        File dumpFile = new File("Guerrilla" + File.separator + "asm/" + ASMUtil.toExternalName(name) + ".class");

        try {
            if (!transform.equals("")) {

                // get transformer class ASM
                ClassReader transformerReader = new ClassReader(transformerClassesCache.get(transform));
                transformerReader.accept(transformerNode, 0);

                for (ITransform iTransform : transforms) {
                    iTransform.transform(classNodeBeingTransformed, transformerNode);
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
            if (transformerNode.name != null) {
                String transformerNodeExternalName = ASMUtil.toExternalName(transformerNode.name);
                String classBeingTransformedExternalName = ASMUtil.toExternalName(name);
                // remap all references from the transformer to class being transformed
                ClassNode temp = new ClassNode();
                ClassVisitor classRemapper = new ClassRemapper(temp, new Remapper() {
                    @Override
                    public String map(String internalName) {
                        if (internalName.equals(transformerNodeExternalName)) {
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
        File dumpFileLog = new File(dumpFile.getPath() + ".dump");
        dumpFileLog.getParentFile().mkdirs();
        FileWriter fileWriter;
        try {
            fileWriter = new FileWriter(dumpFileLog);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        StringWriter sw = new StringWriter();
        PrintWriter printDumpFileLog = new PrintWriter(fileWriter);

        try {
            // write disassembly output to log file
            printDumpFileLog.println("########## Disassembly of Generated ##########");
            TraceClassVisitor traceClassVisitor = new TraceClassVisitor(printDumpFileLog);
            classNode.accept(traceClassVisitor);

        } catch (Exception e) {
            e.printStackTrace();
        }

        // write verification output to log file
        try {
            printDumpFileLog.println("############### Class verifier ###############");

            CheckClassAdapterClassNode.verify(classNode, null, true, printDumpFileLog);

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
            if (fileWriter != null) {
                fileWriter.close();
            }
            if (printDumpFileLog != null) {
                printDumpFileLog.close();
            }
            sw.close();

        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private static boolean containsAnnotation(MethodNode methodNode, Class<? extends Annotation> annotation) {
        if (methodNode.visibleAnnotations == null && methodNode.invisibleAnnotations == null)
            return false;

        if (methodNode.visibleAnnotations != null) {
            for (AnnotationNode visibleAnnotation : methodNode.visibleAnnotations) {
                if (visibleAnnotation.desc.equals(ASMUtil.getClassDescriptor(annotation))) {
                    return true;
                }
            }
        }
        if (methodNode.invisibleAnnotations != null) {
            for (AnnotationNode invisibleAnnotation : methodNode.invisibleAnnotations) {
                if (invisibleAnnotation.desc.equals(ASMUtil.getClassDescriptor(annotation))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean containsAnnotation(FieldNode fieldNode, Class<? extends Annotation> annotation) {
        if (fieldNode.visibleAnnotations == null && fieldNode.invisibleAnnotations == null)
            return false;

        String annotationDesc = ASMUtil.getClassDescriptor(annotation);

        if (fieldNode.visibleAnnotations != null) {
            for (AnnotationNode visibleAnnotation : fieldNode.visibleAnnotations) {
                if (visibleAnnotation.desc.equals(annotationDesc)) {
                    return true;
                }
            }
        }
        if (fieldNode.invisibleAnnotations != null) {
            for (AnnotationNode invisibleAnnotation : fieldNode.invisibleAnnotations) {
                if (invisibleAnnotation.desc.equals(annotationDesc)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static Map<String, String> getTransformMap() {
        return transformMap;
    }
}
