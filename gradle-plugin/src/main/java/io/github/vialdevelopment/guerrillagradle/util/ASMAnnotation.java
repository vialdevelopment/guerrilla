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
    public AnnotationNode originalNode;

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
        originalNode = annotationNode;
    }

    /**
     * Creates an ASM annotation node from this
     */
    public void write() {
        originalNode.desc = this.desc;
        originalNode.values = new ArrayList<>();
        values.forEach((key, value) -> {
            originalNode.values.add(key);
            if (value instanceof ASMAnnotation) {
                ((ASMAnnotation) value).write();
                originalNode.values.add(((ASMAnnotation) value).originalNode);
            } else {
                originalNode.values.add(value);
            }
        });
    }

    /**
     * Gets the annotation value
     * @param name name
     * @return value
     */
    public Object get(String name) {
        return values.get(name);
    }

    public void put(String name, Object value) {
        values.put(name, value);
    }

    public static String getClassDesc(Class<?> clazz) {
        return "L" + clazz.getName().replace('.', '/') + ";";
    }

    /**
     * Gets the annotation of that type in the list of annotations
     * @param annotations annotations list
     * @param clazz annotation class
     * @return annotation or null
     */
    public static ASMAnnotation getAnnotation(List<AnnotationNode> annotations, Class<?> clazz) {
        return getAnnotation(annotations, getClassDesc(clazz));
    }

    /**
     * Gets the annotation of that type in the list of annotations
     * @param annotations annotations list
     * @param desc annotation descriptor
     * @return annotation or null
     */
    public static ASMAnnotation getAnnotation(List<AnnotationNode> annotations, String desc) {
        for (AnnotationNode annotation : annotations) {
            if (annotation.desc.equals(desc)) {
                return new ASMAnnotation(annotation);
            }
        }
        return null;
    }

    /**
     * Gets the annotation on the field node
     * @param fieldNode field node
     * @param desc annotation class desc
     * @return annotation or null
     */
    public static ASMAnnotation getAnnotation(FieldNode fieldNode, String desc) {
        return getAnnotation(combineAnnotationLists(fieldNode.visibleAnnotations, fieldNode.invisibleAnnotations), desc);
    }

    /**
     * Gets the annotation on the method node
     * @param methodNode method node
     * @param desc annotation class desc
     * @return annotation or null
     */
    public static ASMAnnotation getAnnotation(MethodNode methodNode, String desc) {
        return getAnnotation(combineAnnotationLists(methodNode.visibleAnnotations, methodNode.invisibleAnnotations), desc);
    }

    /**
     * Gets the annotation on the method node
     * @param classNode class node
     * @param desc annotation class desc
     * @return annotation or null
     */
    public static ASMAnnotation getAnnotation(ClassNode classNode, String desc) {
        return getAnnotation(combineAnnotationLists(classNode.visibleAnnotations, classNode.invisibleAnnotations), desc);
    }

    public static List<AnnotationNode> combineAnnotationLists(List<AnnotationNode> annotations1, List<AnnotationNode> annotations2) {
        List<AnnotationNode> all = new ArrayList<>();
        if (annotations1 != null) all.addAll(annotations1);
        if (annotations2 != null) all.addAll(annotations2);
        return all;
    }

}
