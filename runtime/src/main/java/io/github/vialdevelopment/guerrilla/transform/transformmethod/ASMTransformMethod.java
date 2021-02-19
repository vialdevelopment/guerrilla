package io.github.vialdevelopment.guerrilla.transform.transformmethod;

import io.github.vialdevelopment.guerrilla.annotation.parse.ASMAnnotation;
import io.github.vialdevelopment.guerrilla.asm.ReflectStaticMethodAccess;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.InvocationTargetException;
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
        System.out.println("Invoking method " + asmAnnotation[0].get("name"));

        try {
            ReflectStaticMethodAccess.generate(transformerClass.name, transformerMethod.name, transformerMethod.desc).invoke(methodBeingTransformed, OBF);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | InstantiationException e) {
            e.printStackTrace();
        }

    }

    @Override
    public List<EAnnotationsUsed> getAnnotations() {
        return annotations;
    }

}
