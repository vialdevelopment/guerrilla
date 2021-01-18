package io.github.vialdevelopment.guerrilla.transform.transformmethod.insert;

import io.github.vialdevelopment.guerrilla.ASMFactory;
import io.github.vialdevelopment.guerrilla.ASMUtil;
import io.github.vialdevelopment.guerrilla.Pattern;
import io.github.vialdevelopment.guerrilla.annotation.insert.At;
import io.github.vialdevelopment.guerrilla.annotation.parse.ASMAnnotation;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Inserts this method's instructions before every return statement
 *
 * The hook method can be static, and takes the arguments of:
 *  - Method being transformed's arguments
 *
 * Internally:
 *  - Hook method prepared for insertion
 *  - Hook method's instructions inserted before every return
 */
public class InsertReturn implements IInsert {

    @Override
    public void insert(ClassNode classBeingTransformed, ClassNode transformerClass, MethodNode methodBeingTransformed, MethodNode transformerMethod, ASMAnnotation insertAnnotation) {
        // is being inserted so needs to be prepared
        ASMUtil.prepareMethodForInsertion(methodBeingTransformed, transformerMethod,
                insertAnnotation.get("returnType") != null ?
                        ASMFactory.EReturnTypes.valueOf(((String[]) insertAnnotation.get("returnType"))[1]) :
                        ASMFactory.EReturnTypes.RETURN);

        // put the instructions before every return
        ASMUtil.insPattBeforeReturn(methodBeingTransformed.instructions, new Pattern(transformerMethod.instructions));
    }

    @Override
    public At.loc type() {
        return At.loc.RETURN;
    }

}
