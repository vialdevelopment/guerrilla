package io.github.vialdevelopment.guerrilla.transform;

import io.github.vialdevelopment.guerrilla.ASMFactory;
import io.github.vialdevelopment.guerrilla.ASMUtil;
import io.github.vialdevelopment.guerrilla.CallBack;
import io.github.vialdevelopment.guerrilla.Pattern;
import io.github.vialdevelopment.guerrilla.annotation.TransformMethod;
import io.github.vialdevelopment.guerrilla.annotation.insert.At;
import io.github.vialdevelopment.guerrilla.annotation.insert.Insert;
import io.github.vialdevelopment.guerrilla.annotation.parse.ASMAnnotation;
import io.github.vialdevelopment.guerrilla.transform.insert.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.github.vialdevelopment.guerrilla.TransformManager.OBF;
import static org.objectweb.asm.Opcodes.*;

/**
 * Processes @TransformMethod,@Insert
 */
public class TransformTransformMethodAndInsert implements ITransform {

    private final List<IInsert> inserters = new ArrayList();

    public TransformTransformMethodAndInsert() {
        inserters.add(new InsertHead());
        inserters.add(new InsertReturn());
        inserters.add(new InsertInvoke());
        inserters.add(new InsertField());
    }

    @Override
    public void transform(ClassNode classBeingTransformed, ClassNode transformerNode) {
        for (MethodNode transformerMethodNode : transformerNode.methods) {
            // get methods with @TransformMethod,@Insert
            ASMAnnotation transformMethodAnnotation = ASMAnnotation.getAnnotation(transformerMethodNode, TransformMethod.class);
            ASMAnnotation insertAnnotation = ASMAnnotation.getAnnotation(transformerMethodNode, Insert.class);

            if (transformMethodAnnotation != null &&
                    insertAnnotation != null) {

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

                At.loc injectionLocation = At.loc.valueOf(((String[]) ((ASMAnnotation)insertAnnotation.get("value")).get("at"))[1]);

                for (IInsert inserter : inserters) {
                    if (inserter.type() == injectionLocation) {
                        inserter.insert(classBeingTransformed, transformerNode, methodBeingTransformed, transformerMethodNode, insertAnnotation);
                    }
                }
            }
        }
    }

    /**
     * Prepares the method for insertion
     * @param insertedIntoMethod method being inserted into
     * @param methodNode method to prepare
     * @param returnType return type from call backs
     */
    public static void prepareMethodForInsertion(MethodNode insertedIntoMethod, MethodNode methodNode, ASMFactory.EReturnTypes returnType) {
        // fixing up the variable references to prevent conflicts works

        int maxVar = 0;
        for (AbstractInsnNode instruction : insertedIntoMethod.instructions) {
            if (instruction instanceof VarInsnNode) {
                if (((VarInsnNode) instruction).var > maxVar) {
                    maxVar = ((VarInsnNode) instruction).var;
                }
            }
        }

        int parameters = Arrays.stream(Type.getArgumentTypes(insertedIntoMethod.desc)).mapToInt(Type::getSize).sum();
        // decrement parameters if static
        parameters = (insertedIntoMethod.access & Opcodes.ACC_STATIC) != 0 ? parameters - 1 : parameters;
        // finally, fix all var references that aren't to this or parameters
        for (AbstractInsnNode instruction : methodNode.instructions) {
            if (instruction instanceof VarInsnNode) {
                if (((VarInsnNode) instruction).var > parameters) {
                    // we do this so that the ASM analyzer doesn't freak out over the changed local variable not being defined already
                    LocalVariableNode localVariableNode = methodNode.localVariables.stream().filter(local -> local.index == ((VarInsnNode) instruction).var).findFirst().orElse(null);
                    if (localVariableNode != null) {
                        localVariableNode.index += maxVar;
                        insertedIntoMethod.localVariables.add(localVariableNode);
                    }

                    ((VarInsnNode) instruction).var += maxVar;
                }
            }
        }

        prepareMethodForInsertion(methodNode, returnType);
    }

    /**
     * Prepare method for insertion
     * @param methodNode method to prepare
     * @param returnType return type from call backs
     */
    public static void prepareMethodForInsertion(MethodNode methodNode, ASMFactory.EReturnTypes returnType) {
        // remove returns
        new Pattern(
                new InsnNode(RETURN)
        ).remove(methodNode.instructions);


        // compiler likes to put in little valueOfs before returns, need to remove them
        // FIXME include all cases of valueOf tricks
        // TODO i don't think we need to remove these, to test
        new Pattern(new MethodInsnNode(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false)).remove(methodNode.instructions);

        // return type can be null if we use annotation's default
        returnType = returnType == null ? ASMFactory.EReturnTypes.RETURN : returnType;

        // now replace calls to CallBack.cancel() with actual returns
        new Pattern(
                new MethodInsnNode(
                        INVOKESTATIC,
                        ASMUtil.getClassExternalName(CallBack.class),
                        "cancel", "(Ljava/lang/Object;)V", false
                )
        ).replace(methodNode.instructions, new Pattern(ASMFactory.generateValueReturn(returnType, null)));
    }

}
