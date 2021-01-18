package io.github.vialdevelopment.guerrilla.transform;

import io.github.vialdevelopment.guerrilla.annotation.Overwrite;
import io.github.vialdevelopment.guerrilla.annotation.TransformIgnoreInline;
import io.github.vialdevelopment.guerrilla.annotation.TransformMethod;
import io.github.vialdevelopment.guerrilla.annotation.insert.Insert;
import io.github.vialdevelopment.guerrilla.annotation.parse.ASMAnnotation;
import io.github.vialdevelopment.guerrilla.transform.transformmethod.*;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import static io.github.vialdevelopment.guerrilla.TransformManager.OBF;

public class TransformMethodEx implements ITransform {

    private static final ITransformMethod noAnnotationTransform = new InlineTransformMethod();
    private static final ITransformMethod insertMethodTransform = new InsertTransformMethod();
    private static final ITransformMethod overwriteMethodTransform = new OverwriteTransformMethod();
    private static final ITransformMethod asmTransformationTransform = new ASMTransformMethod();

    @Override
    public void transform(ClassNode classBeingTransformed, ClassNode transformerNode) {
        for (MethodNode transformerMethod : transformerNode.methods) {
            ASMAnnotation transformMethodAnnotation = ASMAnnotation.getAnnotation(transformerMethod, TransformMethod.class);
            ASMAnnotation insertAnnotation = ASMAnnotation.getAnnotation(transformerMethod, Insert.class);
            ASMAnnotation overwriteAnnotation = ASMAnnotation.getAnnotation(transformerMethod, Overwrite.class);
            ASMAnnotation ignoreInlineAnnotation = ASMAnnotation.getAnnotation(transformerMethod, TransformIgnoreInline.class);

            MethodNode methodBeingTransformed = null;
            if (transformMethodAnnotation != null) {
                // method to target depends on if in obfuscated environment
                final String transformName = (String) (OBF ? transformMethodAnnotation.get("obfMethodName") : transformMethodAnnotation.get("methodName"));
                final String transformArgs = (String) (OBF ? transformMethodAnnotation.get("obfMethodArgs") : transformMethodAnnotation.get("methodArgs"));
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

            if (transformMethodAnnotation == null && insertAnnotation == null && overwriteAnnotation == null && ignoreInlineAnnotation == null) {
                noAnnotationTransform.insert(classBeingTransformed, transformerNode, null, transformerMethod, null);
            } else if (transformerMethod != null && insertAnnotation != null && overwriteAnnotation == null && ignoreInlineAnnotation == null) {
                insertMethodTransform.insert(classBeingTransformed, transformerNode, methodBeingTransformed, transformerMethod, transformMethodAnnotation, insertAnnotation);
            } else if (transformerMethod != null && insertAnnotation == null && overwriteAnnotation != null && ignoreInlineAnnotation == null) {
                overwriteMethodTransform.insert(classBeingTransformed, transformerNode, methodBeingTransformed, transformerMethod, transformMethodAnnotation);
            } else if (transformerMethod != null && insertAnnotation == null && overwriteAnnotation == null && ignoreInlineAnnotation == null) {
                asmTransformationTransform.insert(classBeingTransformed, transformerNode, methodBeingTransformed, transformerMethod, transformMethodAnnotation);
            }
        }

    }

}
