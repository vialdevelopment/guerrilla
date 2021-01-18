package io.github.vialdevelopment.guerrilla.transform;

import org.objectweb.asm.tree.ClassNode;

/**
 * Copies over any interfaces from the transformer class to the class being transformed
 *
 * Internally:
 *  - Copy transformer node's interfaces to class being transformed's interfaces list
 */
public class TransformAddInterfaces implements ITransform {

    @Override
    public void transform(ClassNode classBeingTransformed, ClassNode transformerNode) {
        classBeingTransformed.interfaces.addAll(transformerNode.interfaces);
    }

}
