package io.github.vialdevelopment.guerrilla.transform.insert;

import io.github.vialdevelopment.guerrilla.ASMFactory;
import io.github.vialdevelopment.guerrilla.annotation.insert.At;
import io.github.vialdevelopment.guerrilla.annotation.parse.ASMAnnotation;
import io.github.vialdevelopment.guerrilla.transform.TransformTransformMethodAndInsert;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Inserts the transformer method into the HEAD of the method being transformed
 *
 * This method can be static, and takes the arguments of:
 *  - Method being transformed's arguments
 *
 * Internally:
 *  - Hook method prepare for insertion
 *  - Hook method's instructions inserted at beginning of method being transformed's instructions
 */
public class InsertHead implements IInsert {

    @Override
    public void insert(ClassNode classBeingTransformed, ClassNode transformerClass, MethodNode methodBeingTransformed, MethodNode transformerMethod, ASMAnnotation insertAnnotation) {
        // is being inserted so needs to be prepared
        TransformTransformMethodAndInsert.prepareMethodForInsertion(methodBeingTransformed, transformerMethod,
                insertAnnotation.get("returnType") != null ?
                        ASMFactory.EReturnTypes.valueOf(((String[]) insertAnnotation.get("returnType"))[1]) :
                        ASMFactory.EReturnTypes.RETURN);

        // put the instructions in at the start
        methodBeingTransformed.instructions.insert(transformerMethod.instructions);
    }

    @Override
    public At.loc type() {
        return At.loc.HEAD;
    }

}
