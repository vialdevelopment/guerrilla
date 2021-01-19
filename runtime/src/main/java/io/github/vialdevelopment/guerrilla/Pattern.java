package io.github.vialdevelopment.guerrilla;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;

import java.util.*;

/**
 * Holds the instruction pattern
 */
public class Pattern {
    public List<AbstractInsnNode> patternNodes = new ArrayList<>();

    public Pattern(AbstractInsnNode... nodes) {
        patternNodes.addAll(Arrays.asList(nodes));
        while (patternNodes.remove(null));
    }

    public Pattern(List<AbstractInsnNode> nodes) {
        patternNodes = nodes;
        while (patternNodes.remove(null));
    }

    public Pattern(InsnList insnList) {
        for (int i = 0; i < insnList.size(); i++) {
            patternNodes.add(insnList.get(i));
        }
        while (patternNodes.remove(null));
    }

    /**
     * This takes in a class writer so that i can be lazy and just copy and paste from disassembly window
     * mwahaha
     * @param classWriter the classwriter with only 1 method that contains the pattern
     */
    public Pattern(ClassWriter classWriter) {
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(classWriter.toByteArray());
        classReader.accept(classNode, 0);

        assert classNode.methods.size() == 1;

        InsnList insnList = classNode.methods.get(0).instructions;
        for (int i = 0; i < insnList.size(); i++) {
            patternNodes.add(insnList.get(i));
        }
    }

    public InsnList getInsnList() {
        InsnList insn = new InsnList();
        for (AbstractInsnNode patternNode : patternNodes) {
            insn.add(patternNode);
        }
        return insn;
    }

    /**
     * Matches this pattern in the list
     * @param instructions to check
     * @return pattern found or empty
     */
    public List<List<AbstractInsnNode>> match(InsnList instructions) {
        assert (patternNodes.size() <= instructions.size());

        List<List<AbstractInsnNode>> matches = new ArrayList<>();

        for (int instructionsIndex = 0; instructionsIndex < instructions.size() - patternNodes.size(); instructionsIndex++) {
            List<AbstractInsnNode> matched = new ArrayList<>();
            if (ASMUtil.equalIns(instructions.get(instructionsIndex), patternNodes.get(0))) {
                int patternIndex = 0;
                for (; patternIndex < patternNodes.size(); patternIndex++) {
                    instructions.get(instructionsIndex + patternIndex);
                    if (!ASMUtil.equalIns(instructions.get(instructionsIndex + patternIndex), patternNodes.get(patternIndex)))
                        break;
                }
                if (patternIndex == patternNodes.size()) {
                    // found a match
                    for (int patternIndex2 = 0; patternIndex2 < patternNodes.size(); patternIndex2++) {
                        matched.add(instructions.get(instructionsIndex+patternIndex2));
                    }
                    matches.add(matched);
                }
            }
        }
        return matches;
    }

    /**
     * Duplicates this pattern
     * @return exact copy of this pattern
     */
    public Pattern clone() {
        Map<LabelNode, LabelNode> labels = new HashMap<>();
        for (AbstractInsnNode patternNode : patternNodes) {
            if (patternNode instanceof LabelNode) {
                labels.put((LabelNode) patternNode, new LabelNode());
            }
        }
        List<AbstractInsnNode> newNodes = new ArrayList<>();
        for (AbstractInsnNode patternNode : patternNodes) {
            newNodes.add(patternNode.clone(labels));
        }
        return new Pattern(newNodes);
    }

    /**
     * Replaces this pattern with a different pattern
     * @param instructions ins list to operate on
     * @param replacementPattern to replace this patter with
     */
    public void replace(InsnList instructions, Pattern replacementPattern) {
        List<List<AbstractInsnNode>> allMatches = match(instructions);
        if (allMatches.size() == 0) return;

        for (List<AbstractInsnNode> matched : allMatches) {
            instructions.insertBefore(matched.get(0), replacementPattern.clone().getInsnList());
            for (AbstractInsnNode matchedInsn : matched) {
                instructions.remove(matchedInsn);
            }
        }
    }

    /**
     * Removes this pattern from the list
     * @param instructions insn list to operate on
     */
    public void remove(InsnList instructions) {
        List<List<AbstractInsnNode>> allMatches = match(instructions);
        for (List<AbstractInsnNode> matches : allMatches) {
            for (AbstractInsnNode match : matches) {
                instructions.remove(match);
            }
        }
    }

    /**
     * Inserts this pattern at the beginning of the list
     * @param instructions insn list to operate on
     */
    public void insert(InsnList instructions) {
        instructions.insert(getInsnList());
    }

    /**
     * Replaces the specified instruction in the list with this pattern
     * @param instructions insn list to operate on
     * @param toReplace node to replace
     */
    public void replace(InsnList instructions, AbstractInsnNode toReplace) {
        instructions.insertBefore(toReplace, getInsnList());
        instructions.remove(toReplace);
    }

    /**
     * Inserts this pattern before the offset instruction
     * @param instructions insn list to operate on
     * @param offset instruction this pattern will be places before
     */
    public void insertBefore(InsnList instructions, AbstractInsnNode offset) {
        instructions.insertBefore(offset, getInsnList());
    }

    /**
     * Inserts this pattern before all instructions matching
     * @param instructions insn list to operate on
     * @param toInsertBefore instruction this pattern will be placing before
     */
    public void insertBeforeAll(InsnList instructions, AbstractInsnNode toInsertBefore) {
        new Pattern(toInsertBefore).match(instructions).forEach(matched -> this.clone().insertBefore(instructions, matched.get(0)));
    }

    /**
     * Inserts this pattern after the offset instruction
     * @param instructions insn list to operate on
     * @param offset instruction this pattern will be placed after
     */
    public void insertAfter(InsnList instructions, AbstractInsnNode offset) {
        instructions.insert(offset, getInsnList());
    }

    /**
     * Inserts this pattern after all instructions matching
     * @param instructions insn list to operate on
     * @param toInsertAfter instruction this pattern will be placing after
     */
    public void insertAfterAll(InsnList instructions, AbstractInsnNode toInsertAfter) {
        new Pattern(toInsertAfter).match(instructions).forEach(matched -> this.clone().insertAfter(instructions, matched.get(0)));
    }

    /**
     * Adds together patterns, this + input pattern
     * @param pattern pattern to be added
     * @return pattern with these instructions + the inputted pattern's instructions
     */
    public Pattern add(Pattern pattern) {
        Pattern thisPattern = clone();
        List<AbstractInsnNode> merged = new ArrayList(thisPattern.patternNodes);
        merged.addAll(pattern.clone().patternNodes);
        return new Pattern(merged);
    }

}
