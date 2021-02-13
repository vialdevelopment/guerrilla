package io.github.vialdevelopment.guerrillagradle.tasks;

import io.github.vialdevelopment.guerrillagradle.GuerrillaGradlePlugin;
import io.github.vialdevelopment.guerrillagradle.GuerrillaGradlePluginExtension;
import io.github.vialdevelopment.guerrillagradle.mapping.mapper.api.name.ClassName;
import io.github.vialdevelopment.guerrillagradle.mapping.mapper.api.name.FieldName;
import io.github.vialdevelopment.guerrillagradle.mapping.mapper.api.name.MethodName;
import io.github.vialdevelopment.guerrillagradle.util.ASMAnnotation;
import io.github.vialdevelopment.guerrillagradle.util.NameUtil;
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
import java.util.Map;
import java.util.TreeSet;

/**
 * Fixes the transformer classes to be usable at runtime
 */
public class FixTransformerClassesTask extends DefaultTask {
    /** config extension */
    public GuerrillaGradlePluginExtension extension;
    /** project build directory */
    public File buildClassesDirectory;
    /** classes being transformed */
    public TreeSet<String> alreadyUsedTransformers;
    /** classes transformers are transforming */
    public Map<String, String> transformersTransforming;

    @TaskAction
    public void transform() {
        File transformersFolder = new File(buildClassesDirectory.getPath() + "/" + extension.transformersPackage);
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
                        if (extension.remap) {
                            remapMethodNames(classNode, deobfClassName);
                            remapFieldRedirects(classNode);
                            remapMethodRedirects(classNode);
                        }
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
            transformExtendsAnnotation.visit("clazz", NameUtil.toNormalName(classNode.superName, extension.makePublic));
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
        if (transformClassAnnotation == null) {
            System.out.println("TRANSFORMER CLASS DOES NOT HAVE @TRANSFORMCLASS ANNOTATION!");
            return classNode.name;
        }
        String className = (String) transformClassAnnotation.get("name");
        if (extension.remap && transformClassAnnotation.get("obfName") == null) {
            ClassName remapped = GuerrillaGradlePlugin.mapper.remapClassName(new ClassName(className.replace('.', '/')));
            if (remapped == null) return className;
            transformClassAnnotation.put("obfName", remapped.className.replace('/', '.'));
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
                MethodName remappedName = GuerrillaGradlePlugin.mapper.remapMethodName(new MethodName(deObfClassName, (String) transformMethodAnnotation.get("name"), (String) transformMethodAnnotation.get("desc")));
                if (remappedName == null) continue;
                transformMethodAnnotation.put("obfName", remappedName.methodName);
                transformMethodAnnotation.put("obfDesc", remappedName.methodDesc);

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
            FieldName remappedFieldName = GuerrillaGradlePlugin.mapper.remapFieldName(new FieldName(fieldRefDis[1], fieldRefDis[2], fieldRefDis[3]));
            if (remappedFieldName == null) continue;
            ((ASMAnnotation)insertAnnotation.get("value")).put("obfRef", fieldRefDis[0] + " " + remappedFieldName.ownerName + " " + remappedFieldName.fieldName + " " + remappedFieldName.fieldDesc);
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
            MethodName remappedMethodRef = GuerrillaGradlePlugin.mapper.remapMethodName(new MethodName(methodRefDis[1], methodRefDis[2], methodRefDis[3]));
            // don't continue if there's no mapping for it
            if (remappedMethodRef == null) continue;
            ((ASMAnnotation)insertAnnotation.get("value")).put("obfRef", methodRefDis[0] + " " + remappedMethodRef.ownerName + " " + remappedMethodRef.methodName + " " + remappedMethodRef.methodDesc);
            insertAnnotation.write();
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
