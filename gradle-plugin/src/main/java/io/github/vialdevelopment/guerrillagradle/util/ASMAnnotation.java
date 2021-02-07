package io.github.vialdevelopment.guerrillagradle.util;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A small asm annotation node abstraction
 */
public class ASMAnnotation {
    /** annotation desc */
    public String desc;
    /** annotation values */
    public Map<String, Object> values = new HashMap<>();
    /** original annotation node */
    private AnnotationNode annotationNode;

    /**
     * Constructs this annotation
     * @param annotationNode annotation node
     */
    public ASMAnnotation(AnnotationNode annotationNode) {
        this.desc = annotationNode.desc;
        if (annotationNode.values != null) {
            for (int i = 0; i < annotationNode.values.size(); i+=2) {
                Object value = annotationNode.values.get(i+1);
                if (value instanceof AnnotationNode) {
                    value = new ASMAnnotation((AnnotationNode) value);
                }
                this.values.put((String) annotationNode.values.get(i), value);
            }
        }
        this.annotationNode = annotationNode;
    }

    /**
     * Creates an ASM annotation node from this
     * @return annotation node
     */
    public AnnotationNode write() {
        AnnotationNode newAnnotationNode = new AnnotationNode(this.desc);
        newAnnotationNode.values = new ArrayList<>();
        values.forEach((key, value) -> {
            newAnnotationNode.values.add(key);
            if (value instanceof ASMAnnotation) {
                newAnnotationNode.values.add(((ASMAnnotation) value).write());
            } else {
                newAnnotationNode.values.add(value);
            }
        });
        this.annotationNode.desc = newAnnotationNode.desc;
        this.annotationNode.values = newAnnotationNode.values;
        return newAnnotationNode;
    }

    /**
     * Gets the annotation value
     * @param name name
     * @return value
     */
    public Object get(String name) {
        return values.get(name);
    }

    /**
     * Adds the annotation value
     * @param name name
     * @param value value
     */
    public void put(String name, Object value) {
        values.put(name, value);
    }

    /**
     * Gets the annotation of that type in the list of annotations
     * @param annotations annotations list
     * @param clazz annotation class
     * @return annotation or null
     */
    public static ASMAnnotation getAnnotation(List<AnnotationNode> annotations, Class<?> clazz) {
        String annotationDesc = "L" + clazz.getName().replace('.', '/') + ";";
        return getAnnotation(annotations, annotationDesc);
    }

    /**
     * Gets the annotation of that type in the list of annotations
     * @param annotations annotations list
     * @param annotationDesc annotation class descriptor
     * @return annotation or null
     */
    public static ASMAnnotation getAnnotation(List<AnnotationNode> annotations, String annotationDesc) {
        for (AnnotationNode annotation : annotations) {
            if (annotation.desc.equals(annotationDesc)) {
                return new ASMAnnotation(annotation);
            }
        }
        return null;
    }

    /**
     * Gets the annotation on the class node
     * @param classNode class node
     * @param clazz annotation class
     * @return
     */
    public static ASMAnnotation getAnnotation(ClassNode classNode, Class<?> clazz) {
        return getAnnotation(combineAnnotationLists(classNode.visibleAnnotations, classNode.invisibleAnnotations), clazz);
    }

    /**
     * Gets the annotation on the class node
     * @param classNode class node
     * @param annotationDesc annotation class desc
     * @return
     */
    public static ASMAnnotation getAnnotation(ClassNode classNode, String annotationDesc) {
        return getAnnotation(combineAnnotationLists(classNode.visibleAnnotations, classNode.invisibleAnnotations), annotationDesc);
    }

    /**
     * Gets the annotation on the field node
     * @param fieldNode field node
     * @param clazz annotation class
     * @return annotation or null
     */
    public static ASMAnnotation getAnnotation(FieldNode fieldNode, Class<?> clazz) {
        return getAnnotation(combineAnnotationLists(fieldNode.visibleAnnotations, fieldNode.invisibleAnnotations), clazz);
    }

    /**
     * Gets the annotation on the field node
     * @param fieldNode field node
     * @param annotationDesc annotation class desc
     * @return annotation or null
     */
    public static ASMAnnotation getAnnotation(FieldNode fieldNode, String annotationDesc) {
        return getAnnotation(combineAnnotationLists(fieldNode.visibleAnnotations, fieldNode.invisibleAnnotations), annotationDesc);
    }

    /**
     * Gets the annotation on the method node
     * @param methodNode method node
     * @param clazz annotation class
     * @return annotation or null
     */
    public static ASMAnnotation getAnnotation(MethodNode methodNode, Class<?> clazz) {
        return getAnnotation(combineAnnotationLists(methodNode.visibleAnnotations, methodNode.invisibleAnnotations), clazz);
    }

    /**
     * Gets the annotation on the method node
     * @param methodNode method node
     * @param annotationDesc annotation class desc
     * @return annotation or null
     */
    public static ASMAnnotation getAnnotation(MethodNode methodNode, String annotationDesc) {
        return getAnnotation(combineAnnotationLists(methodNode.visibleAnnotations, methodNode.invisibleAnnotations), annotationDesc);
    }

    public static List<AnnotationNode> combineAnnotationLists(List<AnnotationNode> annotations1, List<AnnotationNode> annotations2) {
        List<AnnotationNode> all = new ArrayList<>();
        if (annotations1 != null) all.addAll(annotations1);
        if (annotations2 != null) all.addAll(annotations2);
        return all;
    }

}
