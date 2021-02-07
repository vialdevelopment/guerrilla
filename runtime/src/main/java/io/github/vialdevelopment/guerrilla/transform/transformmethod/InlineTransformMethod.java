package io.github.vialdevelopment.guerrilla.transform.transformmethod;

import io.github.vialdevelopment.guerrilla.annotation.parse.ASMAnnotation;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Inlines the transformer method
 */
public class InlineTransformMethod implements ITransformMethod {

    private final List<EAnnotationsUsed> annotations = new ArrayList<>();

    @Override
    public void insert(ClassNode classBeingTransformed, ClassNode transformerClass, MethodNode methodBeingTransformed, MethodNode transformerMethod, ASMAnnotation... insertAnnotation) {
        if (!transformerMethod.name.equals("<init>")) { // we handle <init> later
            classBeingTransformed.methods.add(transformerMethod);
        }
    }

    @Override
    public List<EAnnotationsUsed> getAnnotations() {
        return annotations;
    }

}
