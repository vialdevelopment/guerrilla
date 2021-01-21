package io.github.vialdevelopment.guerrilla.transform.transformmethod;

import com.esotericsoftware.reflectasm.MethodAccess;
import io.github.vialdevelopment.guerrilla.annotation.parse.ASMAnnotation;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

import static io.github.vialdevelopment.guerrilla.TransformManager.OBF;

/**
 * Processes only the @TransformMethodEx annotation
 *
 * Invokes the static asm transformer method with the method to transform and if the env is obf
 */
public class ASMTransformMethod implements ITransformMethod {

    private static final List<EAnnotationsUsed> annotations = new ArrayList<>();

    static {
        annotations.add(EAnnotationsUsed.TRANSFORM_METHOD);
    }

    @Override
    public void insert(ClassNode classBeingTransformed, ClassNode transformerClass, MethodNode methodBeingTransformed, MethodNode transformerMethod, ASMAnnotation... asmAnnotation) {
        System.out.println("Invoking method " + asmAnnotation[0].get("methodName"));
        try {
            // object is null because static
            MethodAccess.get(Class.forName(transformerClass.name.replace('/', '.'))).invoke(null, transformerMethod.name, methodBeingTransformed, OBF);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    @Override
    public List<EAnnotationsUsed> getAnnotations() {
        return annotations;
    }

}
