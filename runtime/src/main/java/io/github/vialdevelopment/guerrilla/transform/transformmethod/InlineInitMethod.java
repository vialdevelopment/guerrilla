package io.github.vialdevelopment.guerrilla.transform.transformmethod;

import io.github.vialdevelopment.guerrilla.ASMUtil;
import io.github.vialdevelopment.guerrilla.Pattern;
import io.github.vialdevelopment.guerrilla.annotation.parse.ASMAnnotation;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

public class InlineInitMethod implements ITransformMethod {
    private static final List<EAnnotationsUsed> annotations = new ArrayList<>();

    private List<MethodNode> methods = new ArrayList<>();

    private ASMAnnotation transformExtends;

    @Override
    public void insert(ClassNode classBeingTransformed, ClassNode transformerClass, MethodNode methodBeingTransformed, MethodNode transformerMethod, ASMAnnotation... asmAnnotation) {
        // 3rd annotation is @TransformExtends
        if (asmAnnotation[3] != null && transformerMethod.name.equals("<init>")) methods.add(transformerMethod);
        transformExtends = asmAnnotation[3];
    }

    @Override
    public void end(ClassNode classBeingTransformed, ClassNode transformerClass) {
        if (transformExtends == null) return;

        System.out.println("Inlining <init>s in " + classBeingTransformed.name);

        InsnList[] insnLists = new InsnList[methods.size()];
        for (int i = 0; i < insnLists.length; i++) {
            insnLists[i] = methods.get(i).instructions;
        }

        // remove debug info because they just clash when checking if equal
        for (InsnList insnList : insnLists) {
            ASMUtil.removeDebugInfo(insnList);
        }

        Pattern generalInitInstructions = ASMUtil.findAllBeginningEqualAfterSuper((String) transformExtends.get("clazz"), insnLists);

        for (MethodNode method : classBeingTransformed.methods) {
            if (method.name.equals("<init>")) {
                generalInitInstructions.clone().insertBeforeLast(method.instructions, new InsnNode(Opcodes.RETURN));
            }
        }

        methods = new ArrayList<>();
    }

    @Override
    public List<EAnnotationsUsed> getAnnotations() {
        return annotations;
    }

}
