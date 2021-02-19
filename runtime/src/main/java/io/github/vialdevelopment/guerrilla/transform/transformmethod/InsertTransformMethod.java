package io.github.vialdevelopment.guerrilla.transform.transformmethod;

import io.github.vialdevelopment.guerrilla.TransformManager;
import io.github.vialdevelopment.guerrilla.annotation.insert.At;
import io.github.vialdevelopment.guerrilla.annotation.parse.ASMAnnotation;
import io.github.vialdevelopment.guerrilla.transform.transformmethod.insert.*;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Processes inserting the annotation
 */
public class InsertTransformMethod implements ITransformMethod {

    private static final List<EAnnotationsUsed> annotations = new ArrayList<>();
    private static final List<IInsert> inserters = new ArrayList();

    static {
        annotations.add(EAnnotationsUsed.TRANSFORM_METHOD);
        annotations.add(EAnnotationsUsed.INSERT);

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
     * @param asmAnnotation asm annotations
     */
    @Override
    public void insert(ClassNode classBeingTransformed, ClassNode transformerClass, MethodNode methodBeingTransformed, MethodNode transformerMethod, ASMAnnotation... asmAnnotation) {
        ASMAnnotation transformMethodAnnotation = asmAnnotation[0];
        ASMAnnotation insertAnnotation = asmAnnotation[1];

        TransformManager.LOGGER.verbose("Invoking method " + transformMethodAnnotation.get("name"));

        At.loc injectionLocation = At.loc.valueOf(((String[]) ((ASMAnnotation)insertAnnotation.get("value")).get("at"))[1]);

        for (IInsert inserter : inserters) {
            if (inserter.type() == injectionLocation) {
                inserter.insert(classBeingTransformed, transformerClass, methodBeingTransformed, transformerMethod, insertAnnotation);
            }
        }
    }

    @Override
    public List<EAnnotationsUsed> getAnnotations() {
        return annotations;
    }

}
