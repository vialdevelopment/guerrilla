package io.github.vialdevelopment.guerrilla;

import static org.objectweb.asm.Opcodes.*;

import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import java.util.*;

/**
 * @author NirvanaNevermind
 * Some ASM utils to make the code pretty and life easy
 *
 *
 */
public class ASMUtil {


    public static Map<String, Object> parseAnnotation(AnnotationNode annotationNode) {
        Map<String, Object> annotation = new HashMap<>();
        for (int i = 0; i < annotationNode.values.size(); i+=2) {
            if (annotationNode.values.get(i+1) instanceof String[] && ((String[]) annotationNode.values.get(i+1)).length % 2 == 0) {
                annotation.put((String) annotationNode.values.get(i), parseAnnotation((String[]) annotationNode.values.get(i+1)));
            } else if (annotationNode.values.get(i+1) instanceof AnnotationNode) {
                annotation.put((String) annotationNode.values.get(i), parseAnnotation((AnnotationNode) annotationNode.values.get(i+1)));
            } else {
                annotation.put((String) annotationNode.values.get(i), annotationNode.values.get(i+1));
            }
        }
        return annotation;
    }

    public static Map<String, Object> parseAnnotation(String[] annotationNode) {
        Map<String, Object> annotation = new HashMap<>();
        for (int i = 0; i < annotationNode.length; i+=2) {
            annotation.put(annotationNode[i], annotationNode[i+1]);
        }
        return annotation;
    }

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

        if (node instanceof FieldInsnNode && node2 instanceof FieldInsnNode) {

            return  ((FieldInsnNode) node).owner.equals(((FieldInsnNode) node2).owner) &&
                    ((FieldInsnNode) node).name.equals(((FieldInsnNode) node2).name) &&
                    ((FieldInsnNode) node).desc.equals(((FieldInsnNode) node2).desc);

        } else if (node instanceof FrameNode && node2 instanceof FrameNode) {

            return  ((FrameNode) node).type == ((FrameNode) node2).type &&
                    ((FrameNode) node).local.equals(((FrameNode) node2).local) &&
                    ((FrameNode) node).stack.equals(((FrameNode) node2).stack);

        } else if (node instanceof IincInsnNode && node2 instanceof IincInsnNode) {

            return  ((IincInsnNode) node).var == ((IincInsnNode) node2).var &&
                    ((IincInsnNode) node).incr == ((IincInsnNode) node2).incr;

        } else if (node instanceof InvokeDynamicInsnNode && node2 instanceof InvokeDynamicInsnNode) {

            return ((InvokeDynamicInsnNode) node).name.equals(((InvokeDynamicInsnNode) node2).name) &&
                    ((InvokeDynamicInsnNode) node).desc.equals(((InvokeDynamicInsnNode) node2).desc) &&
                    ((InvokeDynamicInsnNode) node).bsm.equals(((InvokeDynamicInsnNode) node2).bsm) &&
                    Arrays.equals(((InvokeDynamicInsnNode) node).bsmArgs, ((InvokeDynamicInsnNode) node2).bsmArgs);

        } else if (node instanceof JumpInsnNode && node2 instanceof JumpInsnNode) {

            return ((JumpInsnNode) node).label.getLabel().getOffset() == ((JumpInsnNode) node2).label.getLabel().getOffset();

        } else if (node instanceof LabelNode && node2 instanceof LabelNode) {

            return ((LabelNode) node).getLabel().getOffset() == ((LabelNode) node2).getLabel().getOffset();

        } else if (node instanceof LdcInsnNode && node2 instanceof LdcInsnNode) {

            return ((LdcInsnNode) node).cst.equals(((LdcInsnNode) node2).cst);

        } else if (node instanceof LookupSwitchInsnNode && node2 instanceof LookupSwitchInsnNode) {

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

        } else if (node instanceof MethodInsnNode && node2 instanceof MethodInsnNode) {

            return  ((MethodInsnNode) node).owner.equals(((MethodInsnNode) node2).owner) &&
                    ((MethodInsnNode) node).name.equals(((MethodInsnNode) node2).name) &&
                    ((MethodInsnNode) node).desc.equals(((MethodInsnNode) node2).desc);

        } else if (node instanceof MultiANewArrayInsnNode && node2 instanceof MultiANewArrayInsnNode) {

            return ((MultiANewArrayInsnNode) node).desc.equals(((MultiANewArrayInsnNode) node2).desc) &&
                    ((MultiANewArrayInsnNode) node).dims == ((MultiANewArrayInsnNode) node2).dims;

        } else if (node instanceof TableSwitchInsnNode && node2 instanceof TableSwitchInsnNode) {

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

        } else if (node instanceof TypeInsnNode && node2 instanceof TypeInsnNode) {

            return ((TypeInsnNode) node).desc.equals(((TypeInsnNode) node2).desc);

        } else if (node instanceof VarInsnNode && node2 instanceof VarInsnNode) {

            return ((VarInsnNode) node).var == ((VarInsnNode) node2).var;
        }
        // it's just an insnode and opcodes are equal
        return true;
    }

    /**
     * Removes a number of instructions before and itself from the insnlist
     * @param instructions method instructions
     * @param node reference node index
     * @param before number before reference node as well
     */
    public static void removeBeforeS(InsnList instructions, int node, int before) {
        for (int i = 0; i < before + 1; i++) {
            instructions.remove(instructions.get(node-before));
        }
    }

    /**
     * Removes a number of instructions after and itself from the insnlist
     * @param instructions method instructions
     * @param node reference node index
     * @param after number after reference node as well
     */
    public static void removeAfterS(InsnList instructions, int node, int after) {
        for (int i = 0; i < after + 1; i++) {
            instructions.remove(instructions.get(node));
        }
    }

    /**
     * Removes a number of instructions before, after and itself from the instructions list
     * @param instructions instructions list
     * @param node reference node index
     * @param before number before node to be removed
     * @param after number after node to be removed
     */
    public static void removeBeforeAndAfterS(InsnList instructions, int node, int before, int after) {
        for (int i = 0; i < before + after + 1; i++) {
            instructions.remove(instructions.get(node-before));
        }
    }

    /**
     * Removes a number of instructions before from the insnlist
     * @param instructions method instructions
     * @param node reference node index
     * @param before number before reference node as well
     */
    public static void removeBefore(InsnList instructions, int node, int before) {
        for (int i = 0; i < before; i++) {
            instructions.remove(instructions.get(node-before));
        }
    }

    /**
     * Removes a number of instructions after from the insnlist
     * @param instructions method instructions
     * @param node reference node index
     * @param after number after reference node as well
     */
    public static void removeAfter(InsnList instructions, int node, int after) {
        for (int i = 0; i < after; i++) {
            instructions.remove(instructions.get(node+1));
        }
    }

    /**
     * Removes a number of instructions before, after from the instructions list
     * @param instructions instructions list
     * @param node reference node index
     * @param before number before node to be removed
     * @param after number after node to be removed
     */
    public static void removeBeforeAndAfter(InsnList instructions, int node, int before, int after) {
        removeAfter(instructions, node, after);
        removeBefore(instructions, node, before);
    }

    /**
     * Gets all the instructions needed to load a number of args to the function
     * @param classNodeName class node
     * @param methodNode method node
     * @param functionCall call to search before
     * @param loads number of loaded
     * @return instructions list of arg loaders
     */
    public static List<AbstractInsnNode> getParameterLoadingInstructions(String classNodeName, MethodNode methodNode, MethodInsnNode functionCall, int loads) {

        Analyzer<SourceValue> analyzer = new Analyzer<>(new SourceInterpreter());

        try {
            analyzer.analyze(classNodeName, methodNode);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        Frame<SourceValue>[] frames = analyzer.getFrames();
        int end = methodNode.instructions.indexOf(functionCall);
        Frame<SourceValue> sourceValueFrame = frames[end];
        SourceValue receiver = sourceValueFrame.getStack(sourceValueFrame.getStackSize() - loads - 1);
        return new ArrayList<>(receiver.insns);

        /*
        int functionCallIndex = methodNode.instructions.indexOf(functionCall);
        int requiredStack = frames[functionCallIndex].getStackSize() - loads;
        List<AbstractInsnNode> argLoaders = new ArrayList<>();
        for (int i = functionCallIndex-1; i >= 0; i--) {
            argLoaders.add(methodNode.instructions.get(i));
            if (frames[i].getStackSize() == requiredStack) {
                break;
            }
        }
        Collections.reverse(argLoaders);
        return argLoaders;

         */
    }

    /**
     * Counts the number of parameters in a method call
     * @param methodCall method call
     * @return number of parameters
     */
    public static int parameters(MethodInsnNode methodCall) {
        return parameters(methodCall.desc).size();
    }

    /**
     * Gets all the parameters in a method
     * @param methodNode method
     * @return parameters list
     */
    public static List<String> parameters(MethodNode methodNode) {
        return parameters(methodNode.desc);
    }

    /**
     * Gets all the parameters in a desc
     * @param desc method desc
     * @return parameters list
     */
    public static List<String> parameters(String desc) {
        List<String> parameters = new ArrayList<>();

        boolean inName = false;
        StringBuilder current = new StringBuilder();
        for (char c : desc.toCharArray()) {
            if (c == 'L') {
                inName = true;
            } else if (c == ';') {
                inName = false;
                parameters.add(current.toString());
                current = new StringBuilder();

            } else if (!inName) {
                if (c == ')') { // end of parameters
                    break;
                } else if (c != '(') {
                    parameters.add(String.valueOf(c));
                } else {
                    current.append(c);
                }
            }
        }
        return parameters;
    }

    /**
     * Gets the first <init> call in the instructions list
     * @param instructions instructions list
     * @return first <init> call
     */
    public static MethodInsnNode getFirstInit(InsnList instructions) {
        AbstractInsnNode current = instructions.getFirst();
        for (int i = 0; i < instructions.size()-1; i++) {
            if (current instanceof MethodInsnNode) {
                if (((MethodInsnNode) current).name.equals("<init>")) {
                    return (MethodInsnNode) current;
                }
            }
            current = current.getNext();
        }

        return null;
    }

    /**
     * Finds the first equal node in the list
     * @param instructions instructions list
     * @param toFind equal node to find
     * @return node found or null
     */
    public static AbstractInsnNode findFirstEqual(InsnList instructions, AbstractInsnNode toFind) {
        AbstractInsnNode current = instructions.getFirst();
        for (int i = 0; i < instructions.size()-1; i++) {
            if (equalIns(current, toFind)) {
                return current;
            }
            current = current.getNext();
        }
        return null;
    }

}
