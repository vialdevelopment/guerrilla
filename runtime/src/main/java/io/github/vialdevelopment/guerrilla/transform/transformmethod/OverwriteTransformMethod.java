package io.github.vialdevelopment.guerrilla.transform.transformmethod;

import io.github.vialdevelopment.guerrilla.annotation.parse.ASMAnnotation;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Removes the targeted method from the class being transformed and adds the transformer method
 */
public class OverwriteTransformMethod implements ITransformMethod {
    private static final List<EAnnotationsUsed> annotations = new ArrayList<>();

    static {
        annotations.add(EAnnotationsUsed.TRANSFORM_METHOD);
        annotations.add(EAnnotationsUsed.OVERWRITE);
    }

    @Override
    public void insert(ClassNode classBeingTransformed, ClassNode transformerClass, MethodNode methodBeingTransformed, MethodNode transformerMethod, ASMAnnotation... asmAnnotation) {
        classBeingTransformed.methods.remove(methodBeingTransformed);
        classBeingTransformed.methods.add(transformerMethod);
    }

    @Override
    public List<EAnnotationsUsed> getAnnotations() {
        return annotations;
    }

}
