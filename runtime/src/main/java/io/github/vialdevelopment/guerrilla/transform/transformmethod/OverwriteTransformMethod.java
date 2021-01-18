package io.github.vialdevelopment.guerrilla.transform.transformmethod;

import io.github.vialdevelopment.guerrilla.annotation.parse.ASMAnnotation;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Removes the targeted method from the class being transformed and adds the transformer method
 */
public class OverwriteTransformMethod implements ITransformMethod {

    @Override
    public void insert(ClassNode classBeingTransformed, ClassNode transformerClass, MethodNode methodBeingTransformed, MethodNode transformerMethod, ASMAnnotation... asmAnnotation) {
        classBeingTransformed.methods.remove(methodBeingTransformed);
        classBeingTransformed.methods.add(transformerMethod);
    }

}
