package io.github.vialdevelopment.guerrilla.transform;

import com.esotericsoftware.reflectasm.MethodAccess;
import io.github.vialdevelopment.guerrilla.annotation.Overwrite;
import io.github.vialdevelopment.guerrilla.annotation.TransformMethod;
import io.github.vialdevelopment.guerrilla.annotation.insert.Insert;
import io.github.vialdevelopment.guerrilla.annotation.parse.ASMAnnotation;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import static io.github.vialdevelopment.guerrilla.TransformManager.OBF;

/**
 * Processes only the @TransformMethod annotation
 *
 * Invokes the transformer method with the method to transform and if the env is obf
 */
public class TransformTransformMethodOnly implements ITransform {

    @Override
    public void transform(ClassNode classBeingTransformed, ClassNode transformerNode) {
        for (MethodNode transformerMethodNode : transformerNode.methods) {
            // get methods with @TransformMethod and without @Insert, @Overwrite
            ASMAnnotation transformMethodAnnotation = ASMAnnotation.getAnnotation(transformerMethodNode, TransformMethod.class);

            if (transformMethodAnnotation != null &&
                    ASMAnnotation.getAnnotation(transformerMethodNode, Insert.class) == null &&
                    ASMAnnotation.getAnnotation(transformerMethodNode, Overwrite.class) == null) {

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
                // invoke on the correct method
                for (MethodNode refMethod : transformerNode.methods) {
                    if (refMethod.name.equals(transformerNode.name)) {
                        try {
                            // object is null because static
                            MethodAccess.get(Class.forName(transformerNode.name.replace('/', '.'))).invoke(null, transformerMethodNode.name, methodBeingTransformed, OBF);
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }

            }
        }
    }

}
