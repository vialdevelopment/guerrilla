package io.github.vialdevelopment.guerrillagradle.tasks;

import io.github.vialdevelopment.guerrillagradle.Mapper;
import io.github.vialdevelopment.guerrillagradle.util.ASMAnnotation;
import io.github.vialdevelopment.guerrillagradle.util.MiscUtil;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Fixes the transformer classes to be usable at runtime
 */
public class FixTransformerClasses extends DefaultTask {
    /** project build directory */
    public File buildClassesDirectory;
    /** transformers package */
    public String transformers;
    /** mapper instance */
    public Mapper mapper;
    /** classes being transformed */
    public TreeSet<String> alreadyUsedTransformers;
    /** classes transformers are transforming */
    public Map<String, String> transformersTransforming;
    /** make publics list */
    public List<String> makePublics;

    @TaskAction
    public void transform() {
        File transformersFolder = new File(buildClassesDirectory.getPath() + "/" + transformers);
        try {
            // loop over all transformer class files
            Files.walk(Paths.get(transformersFolder.toURI())).forEach(path -> {
                try {
                    // make sure is file and isn't one we generated
                    if (path.toFile().isFile() && path.toString().endsWith(".class")) {
                        // read in the class file
                        byte[] transformerBytes = Files.readAllBytes(path);

                        ClassNode classNode = new ClassNode();
                        ClassReader classReader = new ClassReader(transformerBytes);
                        classReader.accept(classNode, 0);

                        removeSuper(classNode);
                        String deobfClassName = remapClassName(classNode);
                        alreadyUsedTransformers.add(deobfClassName.replace('.', '/'));
                        transformersTransforming.put(classNode.name, deobfClassName.replace('.', '/'));
                        remapMethodNames(classNode, deobfClassName);
                        remapFieldRedirects(classNode);
                        remapMethodRedirects(classNode);
                        remapTransformFieldAccess(classNode, deobfClassName);
                        removeTransformIgnores(classNode);

                        ClassWriter classWriter = new ClassWriter(0);
                        remapReferences(classNode, classWriter, deobfClassName);
                        // write the transformed transformer back
                        Files.write(path, classWriter.toByteArray());
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Removes the super of the transformer
     * @param classNode class node
     */
    private void removeSuper(ClassNode classNode) {
        if (!classNode.superName.equals("java/lang/Object")) {
            AnnotationNode transformExtendsAnnotation = new AnnotationNode("Lio/github/vialdevelopment/guerrilla/annotation/info/TransformerExtends;");
            transformExtendsAnnotation.visit("clazz", MiscUtil.toNormalName(classNode.superName, makePublics));
            classNode.visibleAnnotations.add(transformExtendsAnnotation);
            classNode.superName = "java/lang/Object";
        }
    }

    /**
     * Remaps the @TransformClass annotation
     * @param classNode class node
     * @return
     */
    private String remapClassName(ClassNode classNode) {
        ASMAnnotation transformClassAnnotation = ASMAnnotation.getAnnotation(classNode, "Lio/github/vialdevelopment/guerrilla/annotation/TransformClass;");
        String className = (String) transformClassAnnotation.get("name");
        if (transformClassAnnotation.get("obfName") == null) {
            String remapped = mapper.remapClassName(className);
            if (remapped == null) return className;
            transformClassAnnotation.put("obfName", remapped);
            transformClassAnnotation.write();
        }
        return className;
    }

    /**
     * Remaps the @TransformMethod annotation
     * @param classNode class node
     * @param deObfClassName de-obfuscated class node name
     */
    private void remapMethodNames(ClassNode classNode, String deObfClassName) {
        for (MethodNode method : classNode.methods) {
            ASMAnnotation transformMethodAnnotation = ASMAnnotation.getAnnotation(method, "Lio/github/vialdevelopment/guerrilla/annotation/TransformMethod;");
            if (transformMethodAnnotation == null) continue;
            if (transformMethodAnnotation.get("obfName") == null) {
                String remappedName = mapper.remapMethodName(deObfClassName, (String) transformMethodAnnotation.get("name"), (String) transformMethodAnnotation.get("desc"));
                if (remappedName == null) continue;
                String[] remapped = remappedName.split(" ");
                transformMethodAnnotation.put("obfName", remapped[0]);
                transformMethodAnnotation.put("obfDesc", remapped[1]);

                transformMethodAnnotation.write();
            }
        }
    }

    /**
     * Remaps @Insert field redirects
     * @param classNode class node
     */
    private void remapFieldRedirects(ClassNode classNode) {
        for (MethodNode method : classNode.methods) {
            ASMAnnotation insertAnnotation = ASMAnnotation.getAnnotation(method, "Lio/github/vialdevelopment/guerrilla/annotation/insert/Insert;");
            if (insertAnnotation == null) continue;
            if (!((String[])((ASMAnnotation)insertAnnotation.get("value")).get("at"))[1].equals("FIELD")) continue;
            if (!(((ASMAnnotation)insertAnnotation.get("value")).get("ref") != null && ((ASMAnnotation)insertAnnotation.get("value")).get("obfRef") == null))  continue;

            String[] fieldRefDis = ((String) ((ASMAnnotation) insertAnnotation.get("value")).get("ref")).split(" ");
            String remappedFieldRef = mapper.remapFieldAccess(fieldRefDis[1], fieldRefDis[2]);
            if (remappedFieldRef == null) continue;
            String rebuiltRemapped = fieldRefDis[0] + " " + remappedFieldRef.substring(0, remappedFieldRef.lastIndexOf("/")) + " " + remappedFieldRef.substring(remappedFieldRef.lastIndexOf("/")+1) + " " + fieldRefDis[3];
            ((ASMAnnotation)insertAnnotation.get("value")).put("obfRef", rebuiltRemapped);
            insertAnnotation.write();
        }
    }

    /**
     * Remaps @Insert method redirects
     * @param classNode class node
     */
    private void remapMethodRedirects(ClassNode classNode) {
        for (MethodNode method : classNode.methods) {
            ASMAnnotation insertAnnotation = ASMAnnotation.getAnnotation(method, "Lio/github/vialdevelopment/guerrilla/annotation/insert/Insert;");
            if (insertAnnotation == null) continue;
            if (!((String[])((ASMAnnotation)insertAnnotation.get("value")).get("at"))[1].equals("INVOKE")) continue;
            // only transform if no obf given
            String obfMethodRef = (String) ((ASMAnnotation) insertAnnotation.get("value")).get("obfRef");
            if (obfMethodRef != null || ((ASMAnnotation) insertAnnotation.get("value")).get("ref") == null) continue;
            // get the obf name and add it to the annotation
            String methodRef = (String) ((ASMAnnotation)(insertAnnotation.get("value"))).get("ref");
            String[] methodRefDis = methodRef.split(" ");
            String remappedMethodRef = mapper.remapMethodAccess(methodRefDis[1], methodRefDis[2], methodRefDis[3]);
            // don't continue if there's no mapping for it
            if (remappedMethodRef == null) continue;
            String rebuiltRemapped = methodRefDis[0] + " " + remappedMethodRef.substring(0, remappedMethodRef.lastIndexOf("/")) + " " + remappedMethodRef.substring(remappedMethodRef.lastIndexOf("/")+1);
            ((ASMAnnotation)insertAnnotation.get("value")).put("obfRef", rebuiltRemapped);

            insertAnnotation.write();
        }
    }

    /**
     * Remaps the @TransformFieldAccess
     * @param classNode class node
     * @param deobfClassName de-obfuscated class name
     */
    private void remapTransformFieldAccess(ClassNode classNode, String deobfClassName) {
        for (FieldNode field : classNode.fields) {
            ASMAnnotation transformFieldAccessAnnotation = ASMAnnotation.getAnnotation(field, "Lio/github/vialdevelopment/guerrilla/annotation/TransformFieldAccess;");
            if (transformFieldAccessAnnotation == null) continue;
            if (transformFieldAccessAnnotation.get("obfName") != null) continue;

            String obfName = mapper.remapFieldAccess(deobfClassName, (String) transformFieldAccessAnnotation.get("name"));
            if (obfName == null) continue;
            transformFieldAccessAnnotation.put("obfName", obfName.substring(obfName.lastIndexOf("/")+1));
            transformFieldAccessAnnotation.write();
        }
    }

    /**
     * Removes anything marked with @TransformIgnoreInline
     * @param classNode class node
     */
    private void removeTransformIgnores(ClassNode classNode) {
        for (Iterator<MethodNode> iterator = classNode.methods.iterator(); iterator.hasNext(); ) {
            MethodNode method = iterator.next();
            if (ASMAnnotation.getAnnotation(method, "Lio/github/vialdevelopment/guerrilla/annotation/TransformIgnoreInline;") != null)
                iterator.remove();

        }
        // remove fields marked with @TransformIgnoreInline
        for(Iterator<FieldNode> iterator = classNode.fields.iterator(); iterator.hasNext();) {
            FieldNode fieldNode = iterator.next();
            if (ASMAnnotation.getAnnotation(fieldNode, "Lio/github/vialdevelopment/guerrilla/annotation/TransformIgnoreInline;") != null) {
                iterator.remove();
            }
        }
    }

    private void remapReferences(ClassNode classNode, ClassVisitor classVisitor, String className) {
        // remap all references from the transformer to class being transformed
        className = className.replace('.', '/');
        String originalTransformerName = classNode.name;
        String finalClassName = className;
        ClassVisitor classRemapper = new ClassRemapper(classVisitor, new Remapper() {
            @Override
            public String map(String internalName) {
                return internalName.equals(originalTransformerName) ? finalClassName : internalName;
            }
        }) {
            // overridden to not remap transformer class name
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                cv.visit(version, access, name, remapper.mapSignature(signature, false), remapper.mapType(superName), interfaces == null ? null : remapper.mapTypes(interfaces));
            }
        };
        classNode.accept(classRemapper);
    }

}
