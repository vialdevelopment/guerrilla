package io.github.vialdevelopment.guerrilla;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.Arrays;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

/**
 * @author NirvanaNevermind
 * Some ASM utils to make the code pretty and life easy
 *
 *
 */
public class ASMUtil {

    /**
     * the.package.name
     * @param clazz class
     * @return name
     */
    public static String getClassInternalName(Class clazz) {
        return clazz.getName();
    }

    public static String toDescriptor(String name) {
        return "L" + toExternalName(name) + ";";
    }

    public static String toInternalName(String name) {
        return name.replace('/', '.');
    }

    public static String toExternalName(String name) {
        return name.replace('.', '/');
    }

    /**
     * the/package/name
     * @param clazz class
     * @return name
     */
    public static String getClassExternalName(Class clazz) {
        return getClassInternalName(clazz).replace('.', '/');
    }

    /**
     * Lthe/package/name;
     * @param clazz class
     * @return name
     */
    public static String getClassDescriptor(Class clazz) {
        return "L" + getClassExternalName(clazz) + ";";
    }

    /**
     * Inserts the given pattern before all return statements
     * @param instructions instructions
     * @param pattern pattern to be inserted
     */
    public static void insPattBeforeReturn(InsnList instructions, Pattern pattern) {
        for (int i = 0; i < instructions.size(); i++) {
            AbstractInsnNode current = instructions.get(i);

            switch (current.getOpcode()) {
                case RETURN:
                case ARETURN:
                case DRETURN:
                case FRETURN:
                case IRETURN:
                case LRETURN:
                    pattern.clone().insertBefore(instructions, current);
                    i += pattern.patternNodes.size();
                default:
                    break;
            }
        }
    }

    /**
     * Checks if the 2 nodes are equal
     * @param node node 1
     * @param node2 node 2
     * @return equal
     */
    public static boolean equalIns(AbstractInsnNode node, AbstractInsnNode node2) {
        if (node.getOpcode() != node2.getOpcode()) return false;

        if (node instanceof FieldInsnNode) {

            return  ((FieldInsnNode) node).owner.equals(((FieldInsnNode) node2).owner) &&
                    ((FieldInsnNode) node).name.equals(((FieldInsnNode) node2).name) &&
                    ((FieldInsnNode) node).desc.equals(((FieldInsnNode) node2).desc);

        } else if (node instanceof FrameNode) {

            return  ((FrameNode) node).type == ((FrameNode) node2).type &&
                    ((FrameNode) node).local.equals(((FrameNode) node2).local) &&
                    ((FrameNode) node).stack.equals(((FrameNode) node2).stack);

        } else if (node instanceof IincInsnNode) {

            return  ((IincInsnNode) node).var == ((IincInsnNode) node2).var &&
                    ((IincInsnNode) node).incr == ((IincInsnNode) node2).incr;

        } else if (node instanceof InvokeDynamicInsnNode) {

            return ((InvokeDynamicInsnNode) node).name.equals(((InvokeDynamicInsnNode) node2).name) &&
                    ((InvokeDynamicInsnNode) node).desc.equals(((InvokeDynamicInsnNode) node2).desc) &&
                    ((InvokeDynamicInsnNode) node).bsm.equals(((InvokeDynamicInsnNode) node2).bsm) &&
                    Arrays.equals(((InvokeDynamicInsnNode) node).bsmArgs, ((InvokeDynamicInsnNode) node2).bsmArgs);

        } else if (node instanceof JumpInsnNode) {

            return ((JumpInsnNode) node).label.getLabel().getOffset() == ((JumpInsnNode) node2).label.getLabel().getOffset();

        } else if (node instanceof LabelNode) {

            return ((LabelNode) node).getLabel().getOffset() == ((LabelNode) node2).getLabel().getOffset();

        } else if (node instanceof LdcInsnNode) {

            return ((LdcInsnNode) node).cst.equals(((LdcInsnNode) node2).cst);

        } else if (node instanceof LookupSwitchInsnNode) {

            if (((LookupSwitchInsnNode) node).labels.size() != ((LookupSwitchInsnNode) node2).labels.size())
                return false;

            if (((LookupSwitchInsnNode) node).keys.size() != ((LookupSwitchInsnNode) node2).keys.size())
                return false;

            for (int i = 0; i < ((LookupSwitchInsnNode) node).labels.size(); i++) {
                if (((LookupSwitchInsnNode) node).labels.get(i).getLabel().getOffset() !=
                        ((LookupSwitchInsnNode) node2).labels.get(i).getLabel().getOffset())
                    return false;

            }
            for (int i = 0; i < ((LookupSwitchInsnNode) node).keys.size(); i++) {
                if (!((LookupSwitchInsnNode) node).keys.get(i).equals(((LookupSwitchInsnNode) node2).keys.get(i)))
                    return false;

            }

            return ((LookupSwitchInsnNode) node).dflt.getLabel().getOffset() == ((LookupSwitchInsnNode) node2).dflt.getLabel().getOffset() &&
                    ((LookupSwitchInsnNode) node).keys.equals(((LookupSwitchInsnNode) node2).keys) &&
                    ((LookupSwitchInsnNode) node).labels.equals(((LookupSwitchInsnNode) node2).labels);

        } else if (node instanceof MethodInsnNode) {

            return  ((MethodInsnNode) node).owner.equals(((MethodInsnNode) node2).owner) &&
                    ((MethodInsnNode) node).name.equals(((MethodInsnNode) node2).name) &&
                    ((MethodInsnNode) node).desc.equals(((MethodInsnNode) node2).desc);

        } else if (node instanceof MultiANewArrayInsnNode) {

            return ((MultiANewArrayInsnNode) node).desc.equals(((MultiANewArrayInsnNode) node2).desc) &&
                    ((MultiANewArrayInsnNode) node).dims == ((MultiANewArrayInsnNode) node2).dims;

        } else if (node instanceof TableSwitchInsnNode) {

            if (((TableSwitchInsnNode) node).labels.size() != ((TableSwitchInsnNode) node2).labels.size())
                return false;

            for (int i = 0; i < ((TableSwitchInsnNode) node).labels.size(); i++) {
                if (((TableSwitchInsnNode) node).labels.get(i).getLabel().getOffset() !=
                        ((TableSwitchInsnNode) node2).labels.get(i).getLabel().getOffset())
                    return false;
            }

            return ((TableSwitchInsnNode) node).dflt.getLabel().getOffset() == ((TableSwitchInsnNode) node2).dflt.getLabel().getOffset() &&
                    ((TableSwitchInsnNode) node).min == ((TableSwitchInsnNode) node2).min &&
                    ((TableSwitchInsnNode) node).max == ((TableSwitchInsnNode) node2).max;

        } else if (node instanceof TypeInsnNode) {

            return ((TypeInsnNode) node).desc.equals(((TypeInsnNode) node2).desc);

        } else if (node instanceof VarInsnNode) {

            return ((VarInsnNode) node).var == ((VarInsnNode) node2).var;
        }
        // it's just an insnode and opcodes are equal
        return true;
    }

    /**
     * Makes the hook method static, by appending a "this" parameter onto the end
     * @param classCalledFrom the class the hook method is called from
     * @param methodCalledFrom the method the hook method is called from
     * @param hookMethod the hook method
     */
    public static void makeMethodStatic(ClassNode classCalledFrom, MethodNode methodCalledFrom, MethodNode hookMethod) {
        if ((hookMethod.access & ACC_STATIC) != ACC_STATIC) { // method wasn't static
            // add parameter of a reference to this, if caller isn't static
            if ((methodCalledFrom.access & ACC_STATIC) != ACC_STATIC) {

                StringBuilder descStringBuilder = new StringBuilder(hookMethod.desc);
                descStringBuilder.insert(descStringBuilder.lastIndexOf(")"), "L" + classCalledFrom.name + ";");
                hookMethod.desc = descStringBuilder.toString();

                int parameters = 0;
                for (Type argumentType : Type.getArgumentTypes(hookMethod.desc)) parameters += argumentType.getSize();

                // shift down all var insn as 0 is 1st arg in static
                for (AbstractInsnNode instruction : hookMethod.instructions) {
                    if (instruction instanceof VarInsnNode) ((VarInsnNode) instruction).var += ((VarInsnNode) instruction).var < parameters ? -1 : 1;
                    else if (instruction instanceof IincInsnNode) ((IincInsnNode) instruction).var += ((IincInsnNode) instruction).var < parameters ? -1 : 1;
                }

                // replaces references to this with references to last parameter
                // this shouldn't happen if caller was static
                // TODO throw an error if there is a reference to this in a static caller
                new Pattern(new VarInsnNode(ALOAD, -1)).
                        replace(hookMethod.instructions,
                                new Pattern(new VarInsnNode(ALOAD, parameters-1)));

                // fix local vars so that asm verifier doesn't complain
                hookMethod.localVariables.forEach(localVariableNode -> localVariableNode.index++);
            }
        }
        hookMethod.access = ACC_PUBLIC | ACC_STATIC;
    }

    /**
     * Prepares the method for insertion
     * @param insertedIntoMethod method being inserted into
     * @param methodNode method to prepare
     * @param returnType return type from call backs
     */
    public static void prepareMethodForInsertion(MethodNode insertedIntoMethod, MethodNode methodNode, ASMFactory.EReturnTypes returnType) {
        // fixing up the variable references to prevent conflicts works

        final int[] maxVar = {0};

        insertedIntoMethod.accept(new MethodVisitor(ASM5) {
            @Override
            public void visitVarInsn(int opcode, int var) {
                maxVar[0] = Math.max(maxVar[0], var);
            }
        });

        int parameters = Arrays.stream(Type.getArgumentTypes(insertedIntoMethod.desc)).mapToInt(Type::getSize).sum();
        // decrement parameters if static
        parameters = (insertedIntoMethod.access & Opcodes.ACC_STATIC) != 0 ? parameters - 1 : parameters;

        // finally, fix all var references that aren't to this or parameters
        for (AbstractInsnNode instruction : methodNode.instructions) {
            if (instruction instanceof VarInsnNode && ((VarInsnNode) instruction).var > parameters) ((VarInsnNode) instruction).var += maxVar[0];
            if (instruction instanceof IincInsnNode && ((IincInsnNode) instruction).var > parameters) ((IincInsnNode) instruction).var += maxVar[0];
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

        // return type can be null if we use annotation's default
        returnType = returnType == null ? ASMFactory.EReturnTypes.RETURN : returnType;

        // now replace calls to CallBack.cancel() with actual returns, removing the valueOf tricks
        for (List<AbstractInsnNode> cancel : new Pattern(
                new MethodInsnNode(
                        INVOKESTATIC,
                        ASMUtil.getClassExternalName(CallBack.class),
                        "cancel", "(Ljava/lang/Object;)V", false
                )
        ).match(methodNode.instructions)) {
            AbstractInsnNode previous = cancel.get(0).getPrevious();
            if (previous instanceof MethodInsnNode) {
                if (    equalIns(previous, new MethodInsnNode(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false)) ||
                        equalIns(previous, new MethodInsnNode(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false)) ||
                        equalIns(previous, new MethodInsnNode(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false)) ||
                        equalIns(previous, new MethodInsnNode(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false)) ||
                        equalIns(previous, new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false)) ||
                        equalIns(previous, new MethodInsnNode(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false)) ||
                        equalIns(previous, new MethodInsnNode(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false)) ||
                        equalIns(previous, new MethodInsnNode(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false))
                ) {
                    methodNode.instructions.remove(cancel.get(0).getPrevious());
                }
            }
            new Pattern(ASMFactory.generateValueReturn(returnType, null)).replace(methodNode.instructions, cancel.get(0));
        }
    }

    /**
     * Finds the first equal node in the list
     * @param instructions instructions list
     * @param toFind equal node to find
     * @return node found or null
     */
    public static AbstractInsnNode findFirstEqual(InsnList instructions, AbstractInsnNode toFind) {
        for (AbstractInsnNode instruction : instructions) {
            if (equalIns(instruction, toFind)) return instruction;
        }
        return null;
    }

}
