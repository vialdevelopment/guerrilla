package io.github.vialdevelopment.guerrilla.transform.insert;

import io.github.vialdevelopment.guerrilla.ASMUtil;
import io.github.vialdevelopment.guerrilla.Pattern;
import io.github.vialdevelopment.guerrilla.annotation.insert.At;
import io.github.vialdevelopment.guerrilla.annotation.parse.ASMAnnotation;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import static io.github.vialdevelopment.guerrilla.TransformManager.OBF;
import static org.objectweb.asm.Opcodes.*;

/**
 * Redirects the field reference to the method
 *
 * This hook method has the arguments of:
 *  - Field's owner (if not static field reference)
 * This hook method can be static
 *
 * If the hook method wasn't static it is changed to static:
 *  - Add a parameter to the hook method for "this" object
 *  - Decrement VarInsnNodes referencing parameters
 *  - VarInsnNodes with var -1 are changed to point to the last parameter
 *  - Hook method's access is changed to public static
 *
 * With the hook method now definitely being static:
 *  - Set access to public static
 *  - Get field call being replaced
 *  - Replace field calls with static call to hook method
 *
 */
public class InsertField implements IInsert {

    @Override
    public void insert(ClassNode classBeingTransformed, ClassNode transformerClass, MethodNode methodBeingTransformed, MethodNode transformerMethod, ASMAnnotation insertAnnotation) {
        {
            // inline the hook
            classBeingTransformed.methods.add(transformerMethod);
            // naming scheme
            transformerMethod.name += "hook";
        }

        if ((transformerMethod.access & ACC_STATIC) != ACC_STATIC) { // method wasn't static
            // add parameter of a reference to this, if caller isn't static
            if ((methodBeingTransformed.access & ACC_STATIC) != ACC_STATIC) {
                StringBuilder descStringBuilder = new StringBuilder(transformerMethod.desc);
                descStringBuilder.insert(descStringBuilder.lastIndexOf(")"), "L" + classBeingTransformed.name.replace('.', '/') + ";");
                transformerMethod.desc = descStringBuilder.toString();
            }
            int parameters = 0;
            for (Type argumentType : Type.getArgumentTypes(transformerMethod.desc)) parameters += argumentType.getSize();
            {
                // fix arg references
                for (AbstractInsnNode instruction : transformerMethod.instructions) {
                    if (instruction instanceof VarInsnNode) {
                        if (((VarInsnNode) instruction).var < parameters) {
                            ((VarInsnNode) instruction).var--;
                        } else {
                            ((VarInsnNode) instruction).var++;
                        }
                    }
                }
            }
            {
                // replaces references to this with references to last parameter
                // this shouldn't happen if caller was static
                // TODO throw an error if there is a reference to this in a static caller
                new Pattern(new VarInsnNode(ALOAD, -1)).
                        replace(transformerMethod.instructions,
                                new Pattern(new VarInsnNode(ALOAD, ASMUtil.parameters(transformerMethod.desc).size()-1)));
            }
        }
        {
            // change access
            transformerMethod.access = ACC_PUBLIC | ACC_STATIC;
        }

        // now replace GETFIELDs or GETSTATICs with a call to the hook
        // get the field we're redirecting on
        FieldInsnNode fieldCall;
        {
            String fieldRef = (String) ((ASMAnnotation) insertAnnotation.get("value")).get("fieldRef");
            String obfFieldRef = (String) ((ASMAnnotation) insertAnnotation.get("value")).get("obfFieldRef");
            String[] fieldRegArgs;
            if (obfFieldRef != null) {
                fieldRegArgs = OBF ? obfFieldRef.split(" ") : fieldRef.split(" ");
            } else {
                fieldRegArgs = fieldRef.split(" ");
            }
            fieldCall = new FieldInsnNode(Integer.parseInt(fieldRegArgs[0]), fieldRegArgs[1], fieldRegArgs[2], fieldRegArgs[3]);
        }
        {
            // replace the field getters with calls to hook method
            Pattern hookMethodPattern = new Pattern(
                    new MethodInsnNode(
                            INVOKESTATIC,
                            ASMUtil.toExternalName(classBeingTransformed.name.replace('.', '/')),
                            transformerMethod.name,
                            transformerMethod.desc,
                            false
                    )
            );
            // only load this if not static
            if ((methodBeingTransformed.access & ACC_STATIC) != ACC_STATIC) {
                hookMethodPattern.patternNodes.add(0, new VarInsnNode(ALOAD, 0));
            }
            // replace
            new Pattern(fieldCall).replace(methodBeingTransformed.instructions, hookMethodPattern);
        }
    }

    @Override
    public At.loc type() {
        return At.loc.FIELD;
    }
}
