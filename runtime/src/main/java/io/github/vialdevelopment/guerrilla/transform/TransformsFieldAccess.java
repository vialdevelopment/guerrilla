package io.github.vialdevelopment.guerrilla.transform;

import io.github.vialdevelopment.guerrilla.annotation.TransformFieldAccess;
import io.github.vialdevelopment.guerrilla.annotation.TransformIgnoreInline;
import io.github.vialdevelopment.guerrilla.annotation.parse.ASMAnnotation;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import static io.github.vialdevelopment.guerrilla.TransformManager.OBF;

/**
 * Processes the @TransformFieldAccess annotation
 *
 * Internally:
 *  - Loop over transformer node's fields
 *  - If no @TransformFieldAccess and @TransformIgnoreInline annotations then inline
 *  - Find field to TransformFieldAccess
 *  - Change the access
 */
public class TransformsFieldAccess implements ITransform {

    @Override
    public void transform(ClassNode classBeingTransformed, ClassNode transformerNode) {
        for (FieldNode transformerField : transformerNode.fields) {

            ASMAnnotation transformFieldAccessAnnotation = ASMAnnotation.getAnnotation(transformerField, TransformFieldAccess.class);

            if (transformFieldAccessAnnotation == null) {
                // no @TransformFieldAccess and no @TransformIgnoreInline
                if (ASMAnnotation.getAnnotation(transformerField, TransformIgnoreInline.class) == null) {
                    classBeingTransformed.fields.add(transformerField);
                }
                continue;
            }

            final String transformName = (String) (OBF ? transformFieldAccessAnnotation.get("obfName") : transformFieldAccessAnnotation.get("name"));

            FieldNode fieldToTransform = null;

            for (FieldNode fieldNode : classBeingTransformed.fields) {
                if (fieldNode.name.equals(transformName)) {
                    fieldToTransform = fieldNode;
                    break;
                }
            }

            if (fieldToTransform == null) {
                System.out.println("Field to Transform does not exist!!");
                continue;
            }
            // transforming it is just setting the access
            System.out.println("Invoking Field " + transformFieldAccessAnnotation.get("name"));
            fieldToTransform.access = (int) transformFieldAccessAnnotation.get("access");
        }
    }

}
