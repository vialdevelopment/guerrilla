package io.github.vialdevelopment.guerrilla.transform;

import org.objectweb.asm.tree.ClassNode;

public interface ITransform {
    void transform(ClassNode classBeingTransformed, ClassNode transformerNode);
}
