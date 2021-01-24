package io.github.vialdevelopment.guerrilla;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

/**
 * A collection of factories to make generating ASM code for a patch easier.
 */
public class ASMFactory {

    /**
     * Holds the return types
     */
    public enum EReturnTypes {
        RETURN,
        BOOLEAN,
        STRING,
        LONG,
        DOUBLE,
        OBJECT,
        FLOAT,
        INTEGER,
        BYTE,
        CHAR,
        SHORT
    }

    /**
     * Creates an ASM pattern to fire the event with the specified args
     * Event is stored in var 101 for later use
     * @param event event to be fired
     * @param eventArgs instructions to load arguments for event constructor
     * @param eventDesc description of event init
     * @return asm pattern for this
     */
    public static Pattern fireAndForget(Class event, List<AbstractInsnNode> eventArgs, String eventDesc) {
        String eventName = event.getName().replace('.', '/');

        List<AbstractInsnNode> nodes = new ArrayList<>();
        // get instance of event bus
        nodes.add(new FieldInsnNode(GETSTATIC, "io/github/vialdevelopment/canopy/api/events/Bus", "INSTANCE", "Lio/github/vialdevelopment/canopy/api/events/Bus;"));
        // create the new event
        nodes.add(new TypeInsnNode(NEW, eventName));
        // duplicate so that can use thrice
        nodes.add(new InsnNode(DUP));
        nodes.add(new InsnNode(DUP));
        // get event args
        if (eventArgs != null) {
            nodes.addAll(eventArgs);
        }
        // init the event
        nodes.add(new MethodInsnNode(INVOKESPECIAL, eventName, "<init>", eventDesc != null ? eventDesc : "()V", false));
        // store the event
        nodes.add(new VarInsnNode(ASTORE, 101));
        // dispatch the event
        nodes.add(new MethodInsnNode(INVOKEVIRTUAL, "io/github/vialdevelopment/canopy/api/events/Bus", "dispatch", "(Ljava/lang/Object;)V", false));

        return new Pattern(nodes);
    }

    /**
     * Creates an ASM pattern to fire the event with the specified args and cancel if it is cancelled, returning
     * a specified return type and inserting instructions before to load this return value
     *
     * can have both returns instructions and to return value
     * @param event event to be fired
     * @param returnType return type
     * @param methodDesc instructions to load method arguments
     * @param eventDesc event method init description
     * @param returns instructions to load return value
     * @param toReturn value to return
     * @return asm pattern for this
     */
    public static Pattern fireAndReturnIfCancelledWithArgsAndCustomReturn(Class event, EReturnTypes returnType, List<AbstractInsnNode> methodDesc, String eventDesc, List<AbstractInsnNode> returns, Object toReturn) {
        String eventName = event.getName().replace('.', '/');

        List<AbstractInsnNode> nodes = new ArrayList<>();

        LabelNode labelNode1 = new LabelNode(new Label());

        // create new event
        nodes.add(new TypeInsnNode(NEW, eventName));
        // dup so that can call <init> and store in var 1
        nodes.add(new InsnNode(DUP));
        // load event args
        if (methodDesc != null) {
            nodes.addAll(methodDesc);
        }
        // init the event
        nodes.add(new MethodInsnNode(INVOKESPECIAL, eventName, "<init>", eventDesc, false));
        // store event in var 101
        nodes.add(new VarInsnNode(ASTORE, 101));
        // get event bus instance
        nodes.add(new FieldInsnNode(GETSTATIC, "io/github/vialdevelopment/canopy/api/events/Bus", "INSTANCE", "Lio/github/vialdevelopment/canopy/api/events/Bus;"));
        // load event var
        nodes.add(new VarInsnNode(ALOAD, 101));
        // dispatch event
        nodes.add(new MethodInsnNode(INVOKEVIRTUAL, "io/github/vialdevelopment/canopy/api/events/Bus", "dispatch", "(Ljava/lang/Object;)V", false));
        // load event var
        nodes.add(new VarInsnNode(ALOAD, 101));
        // check if event is cancelled
        nodes.add(new MethodInsnNode(INVOKEVIRTUAL, eventName, "isCanceled", "()Z", false));
        // if event is not cancelled then jump
        nodes.add(new JumpInsnNode(IFEQ, labelNode1));
        // load return args
        if (returns != null) {
            nodes.addAll(returns);
        }
        // return if event was cancelled
        nodes.addAll(generateValueReturn(returnType, toReturn));
        // if event not cancelled then jump here and continue with rest of function
        nodes.add(labelNode1);

        return new Pattern(nodes);
    }

    /**
     * Creates an ASM pattern to fire the event and cancel if it is cancelled with a specified return value
     * @param event event to be fired
     * @param returnType type of return
     * @param toReturn value to return or null if RETURN
     * @return asm pattern for this
     */
    public static Pattern fireAndReturnIfCancelled(Class event, EReturnTypes returnType, Object toReturn) {
        String eventName = event.getName().replace('.', '/');

        List<AbstractInsnNode> nodes = new ArrayList<>();

        LabelNode labelNode1 = new LabelNode(new Label());

        // create new event
        nodes.add(new TypeInsnNode(NEW, eventName));
        // dup so that can call <init> and store in var 1
        nodes.add(new InsnNode(DUP));
        // init the event
        nodes.add(new MethodInsnNode(INVOKESPECIAL, eventName, "<init>", "()V", false));
        // store event in var 1
        nodes.add(new VarInsnNode(ASTORE, 101));
        // get event bus instance
        nodes.add(new FieldInsnNode(GETSTATIC, "io/github/vialdevelopment/canopy/api/events/Bus", "INSTANCE", "Lio/github/vialdevelopment/canopy/api/events/Bus;"));
        // load event var
        nodes.add(new VarInsnNode(ALOAD, 101));
        // dispatch event
        nodes.add(new MethodInsnNode(INVOKEVIRTUAL, "io/github/vialdevelopment/canopy/api/events/Bus", "dispatch", "(Ljava/lang/Object;)V", false));
        // load event var
        nodes.add(new VarInsnNode(ALOAD, 101));
        // check if event is cancelled
        nodes.add(new MethodInsnNode(INVOKEVIRTUAL, eventName, "isCanceled", "()Z", false));
        // if event is not cancelled then jump
        nodes.add(new JumpInsnNode(IFEQ, labelNode1));
        // return if event was cancelled
        nodes.addAll(generateValueReturn(returnType, toReturn));
        // if event not cancelled then jump here and continue with rest of function
        nodes.add(labelNode1);

        return new Pattern(nodes);
    }

    /**
     * Creates an ASM pattern to check if the specified module is enabled and if so return the specified value
     * @param module module to check
     * @param returnType type of return value
     * @param toReturn value to return, can be null for RETURN
     * @return asm pattern for this
     */
    public static Pattern returnIfModuleEnabled(Class module, EReturnTypes returnType, Object toReturn) {
        String moduleName = module.getName().replace('.', '/');
        LabelNode labelNode1 = new LabelNode(new Label());

        List<AbstractInsnNode> nodes = new ArrayList<>();
        // get module instance
        nodes.add(new FieldInsnNode(GETSTATIC, moduleName, "INSTANCE", "L" + moduleName + ";"));
        // get module state
        nodes.add(new MethodInsnNode(INVOKEVIRTUAL, moduleName, "getState", "()Z", false));
        // if module not enabled then jump ahead
        nodes.add(new JumpInsnNode(IFEQ, labelNode1));
        // return if module enabled
        nodes.addAll(generateValueReturn(returnType, toReturn));
        // label to continue on
        nodes.add(labelNode1);

        return new Pattern(nodes);
    }

    public static Pattern loadAllMethodArgs(String desc, boolean staticCaller) {
        List<AbstractInsnNode> loaders = new ArrayList<>();
        Type[] argumentTypes = Type.getArgumentTypes(desc);
        int counter = 0;
        for (Type argumentType : argumentTypes) {
            loaders.add(new VarInsnNode(argumentType.getOpcode(ILOAD), counter + (staticCaller ? 0 : 1)));
            counter += argumentType.getSize();
        }
        return new Pattern(loaders);
    }

    /**
     * Generates a return for the value, loading the value and returning with the appropriate instructions
     * @param returnType type of return
     * @param toReturn object to return, or null if replace later
     * @return list of abstract insn nodes
     */
    public static List<AbstractInsnNode> generateValueReturn(EReturnTypes returnType, Object toReturn) {
        List<AbstractInsnNode> nodes = new ArrayList<>();

        if (returnType == EReturnTypes.RETURN) {
            nodes.add(new InsnNode(RETURN));
        } else {
            if (toReturn != null) {
                nodes.add(new LdcInsnNode(toReturn));
            }
            switch (returnType) {
                case DOUBLE:
                    nodes.add(new InsnNode(DRETURN));
                    break;
                case STRING:
                    nodes.add(new InsnNode(ARETURN));
                    break;
                case LONG:
                    nodes.add(new InsnNode(LRETURN));
                    break;
                case OBJECT:
                    nodes.add(new InsnNode(ARETURN));
                    break;
                case BOOLEAN:
                    // nodes.add(new InsnNode((boolean)toReturn ? ICONST_1 : ICONST_0));
                    nodes.add(new InsnNode(IRETURN));
                    break;
                case INTEGER:
                    nodes.add(new InsnNode(IRETURN));
                    break;
                case FLOAT:
                    nodes.add(new InsnNode(FRETURN));
                    break;
                case BYTE:
                    nodes.add(new InsnNode(IRETURN));
                    break;
                case CHAR:
                    nodes.add(new InsnNode(IRETURN));
                    break;
                case SHORT:
                    nodes.add(new InsnNode(IRETURN));
                    break;
            }

        }


        return nodes;
    }

}
