package io.github.vialdevelopment.guerrilla.transform;

import io.github.vialdevelopment.guerrilla.annotation.Overwrite;
import io.github.vialdevelopment.guerrilla.annotation.TransformIgnoreInline;
import io.github.vialdevelopment.guerrilla.annotation.TransformMethod;
import io.github.vialdevelopment.guerrilla.annotation.info.TransformerExtends;
import io.github.vialdevelopment.guerrilla.annotation.insert.Insert;
import io.github.vialdevelopment.guerrilla.annotation.parse.ASMAnnotation;
import io.github.vialdevelopment.guerrilla.transform.transformmethod.*;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

import static io.github.vialdevelopment.guerrilla.TransformManager.OBF;

public class TransformMethodEx implements ITransform {

    private static final ITransformMethod[] transforms = new ITransformMethod[] { new ASMTransformMethod(), new InlineTransformMethod(), new InsertTransformMethod(), new OverwriteTransformMethod(), new InlineInitMethod() };

    @Override
    public void transform(ClassNode classBeingTransformed, ClassNode transformerClass) {
        ASMAnnotation transformerExtendsAnnotation = ASMAnnotation.getAnnotation(transformerClass, TransformerExtends.class);
        for (MethodNode transformerMethod : transformerClass.methods) {
            ASMAnnotation transformMethodAnnotation = ASMAnnotation.getAnnotation(transformerMethod, TransformMethod.class);
            ASMAnnotation insertAnnotation = ASMAnnotation.getAnnotation(transformerMethod, Insert.class);
            ASMAnnotation overwriteAnnotation = ASMAnnotation.getAnnotation(transformerMethod, Overwrite.class);
            ASMAnnotation ignoreInlineAnnotation = ASMAnnotation.getAnnotation(transformerMethod, TransformIgnoreInline.class);

            MethodNode methodBeingTransformed = null;
            if (transformMethodAnnotation != null) {
                // method to target depends on if in obfuscated environment
                final String transformName = (String) (OBF ? transformMethodAnnotation.get("obfName") : transformMethodAnnotation.get("name"));
                final String transformArgs = (String) (OBF ? transformMethodAnnotation.get("obfDesc") : transformMethodAnnotation.get("desc"));
                // get the method node targeted
                for (Object object : classBeingTransformed.methods) {
                    final MethodNode methodNode = (MethodNode) object;
                    if (methodNode.name.equals(transformName)
                            && methodNode.desc.equals(transformArgs)) {
                        methodBeingTransformed = methodNode;
                        break;
                    }

                }
            }

            List<EAnnotationsUsed> annotationsUsedList = new ArrayList<>();
            if (transformMethodAnnotation != null) annotationsUsedList.add(EAnnotationsUsed.TRANSFORM_METHOD);
            if (insertAnnotation != null) annotationsUsedList.add(EAnnotationsUsed.INSERT);
            if (overwriteAnnotation != null) annotationsUsedList.add(EAnnotationsUsed.OVERWRITE);
            if (ignoreInlineAnnotation != null) annotationsUsedList.add(EAnnotationsUsed.IGNORE_INLINE);

            for (ITransformMethod transform : transforms) {
                List<EAnnotationsUsed> transformAnnotations = transform.getAnnotations();
                if (annotationsUsedList.size() != transformAnnotations.size()) continue;
                if (transformAnnotations.containsAll(annotationsUsedList) && annotationsUsedList.containsAll(transformAnnotations)) {
                    transform.insert(classBeingTransformed, transformerClass, methodBeingTransformed, transformerMethod, transformMethodAnnotation, insertAnnotation, overwriteAnnotation, transformerExtendsAnnotation);
                }
            }
        }

        for (ITransformMethod transform : transforms) {
            transform.end(classBeingTransformed, transformerClass);
        }
    }

}
