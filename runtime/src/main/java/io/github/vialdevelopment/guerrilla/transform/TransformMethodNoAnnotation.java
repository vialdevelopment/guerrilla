package io.github.vialdevelopment.guerrilla.transform;

import io.github.vialdevelopment.guerrilla.annotation.parse.ASMAnnotation;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Processes methods without annotations
 *
 * Internally:
 *  - Loop through transformer's methods
 *  - If method has no annotations, inline
 */
public class TransformMethodNoAnnotation implements ITransform {

    @Override
    public void transform(ClassNode classBeingTransformed, ClassNode transformerNode) {
        for (MethodNode transformerMethodNode : transformerNode.methods) {
            // if method doesn't have any annotations, just in line it
            if (ASMAnnotation.combineAnnotationLists(transformerMethodNode.visibleAnnotations, transformerMethodNode.invisibleAnnotations).size() == 0) {
                classBeingTransformed.methods.add(transformerMethodNode);
            }
        }
    }

}
