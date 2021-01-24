package io.github.vialdevelopment.guerrilla.transform.transformmethod.insert;

import io.github.vialdevelopment.guerrilla.ASMFactory;
import io.github.vialdevelopment.guerrilla.ASMUtil;
import io.github.vialdevelopment.guerrilla.Pattern;
import io.github.vialdevelopment.guerrilla.annotation.insert.At;
import io.github.vialdevelopment.guerrilla.annotation.parse.ASMAnnotation;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;

import static io.github.vialdevelopment.guerrilla.TransformManager.OBF;
import static org.objectweb.asm.Opcodes.*;

/**
 * Inserts in or around the method invoke
 *
 * This deals with 2 types of insertion, a mangled static hook, and insertion
 *
 * With an invoke offset of 0, we are replacing the invoke call with a call to our method
 *
 * This hook method will not be static in writing, and will take the parameters:
 *  - Object being called on if not static
 *  - Arguments to method invoke being replaced
 *  - Method being transformed's arguments.
 *
 * Internally, this hook method is mangled.
 *  - Add the hook method node to the class being transformed
 *  - "Hook" appended to name
 *  - Access set to public static
 *  - Add argument to hook method for reference to "this"
 *  - VarInsnNodes referencing parameters are decremented
 *  - VarInsnNodes previously referencing "this" are now at -1, change these to reference the last parameter
 *  - Replace the call with a static call to the hook, inserting instructions to load the method parameters and the "this" object onto a stack before
 *
 *
 * With an invoke offset of not 0, we are inserting the hook's code into our method
 *
 * This hook will take the parameters:
 *  - Method being transformed's arguments
 *
 * This is done by:
 *  - Removing hook's RETURN instructions
 *  - Removing valueOf tricks before returns
 *  - Replace calls to CallBack.cancel(value) with an automatically generated return statement based off type
 *  - Inserts the fixed hook's code around the specified offset
 */
public class InsertInvoke implements IInsert {

    @Override
    public void insert(ClassNode classBeingTransformed, ClassNode transformerClass, MethodNode methodBeingTransformed, MethodNode transformerMethod, ASMAnnotation insertAnnotation) {

        int invokeOffset = ((ASMAnnotation)insertAnnotation.get("value")).get("offset") == null ? 0 : (int) ((ASMAnnotation)insertAnnotation.get("value")).get("offset");

        // get the method we're redirecting on
        MethodInsnNode methodCall;
        {
            String injectMethodCall = (String) ((ASMAnnotation)insertAnnotation.get("value")).get("methodCall");
            String injectObfMethodCall = (String) ((ASMAnnotation)insertAnnotation.get("value")).get("obfMethodCall");
            String[] methodCallArgs;
            if (injectObfMethodCall != null) {
                methodCallArgs = OBF ? injectObfMethodCall.split(" ") : injectMethodCall.split(" ");
            } else {
                methodCallArgs = injectMethodCall.split(" ");
            }
            methodCall = new MethodInsnNode(Integer.parseInt(methodCallArgs[0]), methodCallArgs[1], methodCallArgs[2], methodCallArgs[3]);
        }

        if (invokeOffset == 0) {
            // if the invoke offset is 0 then we inline to hook and replace the call
            // instructions not prepared for insertion
            {
                // inline hook method
                classBeingTransformed.methods.add(transformerMethod);
                // naming scheme
                transformerMethod.name += "Hook";
            }
            ASMUtil.makeMethodStatic(classBeingTransformed, methodBeingTransformed, transformerMethod);
            {
                // replace references
                Pattern newMethodCall = new Pattern(
                        new MethodInsnNode(
                                INVOKESTATIC,
                                classBeingTransformed.name,
                                transformerMethod.name,
                                transformerMethod.desc));

                boolean staticCaller = (methodBeingTransformed.access & ACC_STATIC) == ACC_STATIC;
                // if non static caller, load this
                if (!staticCaller) {
                    newMethodCall.patternNodes.add(0, new VarInsnNode(ALOAD, 0));
                }
                // load caller's args
                List<AbstractInsnNode> loaders = new ArrayList<>();
                Type[] argumentTypes = Type.getArgumentTypes(methodBeingTransformed.desc);
                int counter = 0;
                for (int i = 0; i < argumentTypes.length; i++) {
                    loaders.add(new VarInsnNode(argumentTypes[i].getOpcode(ILOAD), counter+(staticCaller?0:1)));
                    counter += argumentTypes[i].getSize();
                }
                newMethodCall.patternNodes.addAll(0, loaders);
                // finally, replace the calls
                new Pattern(methodCall).replace(methodBeingTransformed.instructions, newMethodCall);
            }
        } else {

            // we're inserting the instructions somewhere around the invoke
            // instructions have been prepared for insertion
            ASMUtil.prepareMethodForInsertion(methodBeingTransformed, transformerMethod,
                    insertAnnotation.get("returnType") != null ?
                            ASMFactory.EReturnTypes.valueOf(((String[]) insertAnnotation.get("returnType"))[1]) :
                            ASMFactory.EReturnTypes.RETURN);

            if (invokeOffset < 0) { // insert before
                for (AbstractInsnNode current : methodBeingTransformed.instructions) {
                    if (ASMUtil.equalIns(current, methodCall)) {
                        AbstractInsnNode toInsertBefore = current;
                        for (int i1 = 0; i1 <= invokeOffset - 1; i1--) {
                            toInsertBefore = toInsertBefore.getPrevious();
                        }
                        new Pattern(transformerMethod.instructions).insertBefore(methodBeingTransformed.instructions, toInsertBefore);
                    }
                }

            } else { // insert after
                for (AbstractInsnNode current : methodBeingTransformed.instructions) {
                    if (ASMUtil.equalIns(current, methodCall)) {
                        AbstractInsnNode toInsertAfter = current;
                        for (int i1 = 0; i1 < invokeOffset - 1; i1++) {
                            toInsertAfter = toInsertAfter.getNext();
                        }
                        new Pattern(transformerMethod.instructions).insertAfter(transformerMethod.instructions, toInsertAfter);
                    }
                }
            }
        }
    }

    @Override
    public At.loc type() {
        return At.loc.INVOKE;
    }

}
