package io.github.vialdevelopment.guerrilla.transform.transformmethod;

import io.github.vialdevelopment.guerrilla.ASMFactory;
import io.github.vialdevelopment.guerrilla.ASMUtil;
import io.github.vialdevelopment.guerrilla.CallBack;
import io.github.vialdevelopment.guerrilla.Pattern;
import io.github.vialdevelopment.guerrilla.annotation.insert.At;
import io.github.vialdevelopment.guerrilla.annotation.parse.ASMAnnotation;
import io.github.vialdevelopment.guerrilla.transform.transformmethod.insert.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.RETURN;

/**
 * Processes inserting the annotation
 */
public class InsertTransformMethod implements ITransformMethod {

    private static final List<IInsert> inserters = new ArrayList();

    static {
        inserters.add(new InsertHead());
        inserters.add(new InsertReturn());
        inserters.add(new InsertInvoke());
        inserters.add(new InsertField());
    }

    /**
     * @param classBeingTransformed class being transformed
     * @param transformerClass transformer class
     * @param methodBeingTransformed method being transformed
     * @param transformerMethod transformer method
     * @param asmAnnotation asm annotations [ @TransformMethodEx, @Insert ]
     */
    @Override
    public void insert(ClassNode classBeingTransformed, ClassNode transformerClass, MethodNode methodBeingTransformed, MethodNode transformerMethod, ASMAnnotation... asmAnnotation) {
        ASMAnnotation transformMethodAnnotation = asmAnnotation[0];
        ASMAnnotation insertAnnotation = asmAnnotation[1];

        System.out.println("Invoking method " + transformMethodAnnotation.get("methodName"));

        At.loc injectionLocation = At.loc.valueOf(((String[]) ((ASMAnnotation)insertAnnotation.get("value")).get("at"))[1]);

        for (IInsert inserter : inserters) {
            if (inserter.type() == injectionLocation) {
                inserter.insert(classBeingTransformed, transformerClass, methodBeingTransformed, transformerMethod, insertAnnotation);
            }
        }
    }

}
