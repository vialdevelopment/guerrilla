package io.github.vialdevelopment.guerrilla.transform;

import io.github.vialdevelopment.guerrilla.annotation.Overwrite;
import io.github.vialdevelopment.guerrilla.annotation.TransformMethod;
import io.github.vialdevelopment.guerrilla.annotation.parse.ASMAnnotation;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import static io.github.vialdevelopment.guerrilla.TransformManager.OBF;

/**
 * This lets us overwrite methods!
 *
 * Internally:
 *  - Loop over transformer methods
 *  - Check if has @TransformMethod and @Overwrite annotation
 *  - Find method being transformed
 *  - Remove it and replace with transformer method
 */
public class TransformTransformMethodAndOverwrite implements ITransform {

    @Override
    public void transform(ClassNode classBeingTransformed, ClassNode transformerNode) {
        for (MethodNode transformerMethodNode : transformerNode.methods) {
            // get methods with @TransformMethod,@Overwrite
            ASMAnnotation transformMethodAnnotation = ASMAnnotation.getAnnotation(transformerMethodNode, TransformMethod.class);

            if (transformMethodAnnotation != null &&
                    ASMAnnotation.getAnnotation(transformerMethodNode, Overwrite.class) != null) {

                // method to target depends on if in obfuscated environment
                final String transformName = (String) (OBF ? transformMethodAnnotation.get("obfMethodName") : transformMethodAnnotation.get("methodName"));

                final String transformArgs = (String) (OBF ? transformMethodAnnotation.get("obfMethodArgs") : transformMethodAnnotation.get("methodArgs"));

                MethodNode methodBeingTransformed = null;
                // get the method node targeted
                for (Object object : classBeingTransformed.methods) {
                    final MethodNode methodNode = (MethodNode) object;

                    if (methodNode.name.equals(transformName)
                            && methodNode.desc.equals(transformArgs)) {
                        methodBeingTransformed = methodNode;
                        break;
                    }

                }

                if (methodBeingTransformed == null) {
                    System.out.println("FinalMethodNode does not exist!!");
                    continue;
                }

                System.out.println("Invoking method " + transformMethodAnnotation.get("methodName"));

                classBeingTransformed.methods.remove(methodBeingTransformed);
                classBeingTransformed.methods.add(transformerMethodNode);
            }
        }
    }
}
