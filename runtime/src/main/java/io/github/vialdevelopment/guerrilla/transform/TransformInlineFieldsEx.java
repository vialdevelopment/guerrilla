package io.github.vialdevelopment.guerrilla.transform;

import io.github.vialdevelopment.guerrilla.annotation.TransformIgnoreInline;
import io.github.vialdevelopment.guerrilla.annotation.parse.ASMAnnotation;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

/**
 * Inlines fields without the @TransformIgnoreInline annotation
 */
public class TransformInlineFieldsEx implements ITransform {

    @Override
    public void transform(ClassNode classBeingTransformed, ClassNode transformerNode) {
        for (FieldNode field : transformerNode.fields) {
            if (ASMAnnotation.getAnnotation(field, TransformIgnoreInline.class) == null) {
                classBeingTransformed.fields.add(field);
            }
        }
    }

}
