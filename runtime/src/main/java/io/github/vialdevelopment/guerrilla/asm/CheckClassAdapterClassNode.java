package io.github.vialdevelopment.guerrilla.asm;

import org.mutabilitydetector.asm.tree.analysis.NonClassloadingSimpleVerifier;
import org.mutabilitydetector.asm.typehierarchy.TypeHierarchyReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.analysis.*;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class CheckClassAdapterClassNode extends CheckClassAdapter {

    public CheckClassAdapterClassNode(ClassVisitor classVisitor) {
        super(classVisitor);
    }

    /**
     * Checks the given class.
     *
     * @param classNode the class to be checked.
     * @param loader a <code>ClassLoader</code> which will be used to load referenced classes. May be
     *     {@literal null}.
     * @param printResults whether to print the results of the bytecode verification.
     * @param printWriter where the results (or the stack trace in case of error) must be printed.
     */
    public static void verify(
            final ClassNode classNode,
            final ClassLoader loader,
            final boolean printResults,
            final PrintWriter printWriter) {

        Type syperType = classNode.superName == null ? null : Type.getObjectType(classNode.superName);
        List<MethodNode> methods = classNode.methods;

        List<Type> interfaces = new ArrayList<>();
        for (String interfaceName : classNode.interfaces) {
            interfaces.add(Type.getObjectType(interfaceName));
        }

        for (MethodNode method : methods) {
            NonClassloadingSimpleVerifier verifier =
                    new NonClassloadingSimpleVerifier(
                            Type.getObjectType(classNode.name),
                            syperType,
                            interfaces,
                            (classNode.access & Opcodes.ACC_INTERFACE) != 0,
                            new TypeHierarchyReader());

            Analyzer<BasicValue> analyzer = new Analyzer<>(verifier);
            if (loader != null) {
                verifier.setClassLoader(loader);
            }
            try {
                analyzer.analyze(classNode.name, method);
            } catch (AnalyzerException e) {
                e.printStackTrace(printWriter);
            }
            if (printResults) {
                printAnalyzerResult(method, analyzer, printWriter);
            }
        }
        printWriter.flush();
    }

    static void printAnalyzerResult(
            final MethodNode method, final Analyzer<BasicValue> analyzer, final PrintWriter printWriter) {
        Textifier textifier = new Textifier();
        TraceMethodVisitor traceMethodVisitor = new TraceMethodVisitor(textifier);

        printWriter.println(method.name + method.desc);
        for (int i = 0; i < method.instructions.size(); ++i) {
            if (method.instructions.get(i) == null) continue;
            method.instructions.get(i).accept(traceMethodVisitor);

            StringBuilder stringBuilder = new StringBuilder();
            Frame<BasicValue> frame = analyzer.getFrames()[i];
            if (frame == null) {
                stringBuilder.append('?');
            } else {
                for (int j = 0; j < frame.getLocals(); ++j) {
                    stringBuilder.append(getUnqualifiedName(frame.getLocal(j).toString())).append(' ');
                }
                stringBuilder.append(" : ");
                for (int j = 0; j < frame.getStackSize(); ++j) {
                    stringBuilder.append(getUnqualifiedName(frame.getStack(j).toString())).append(' ');
                }
            }
            while (stringBuilder.length() < method.maxStack + method.maxLocals + 1) {
                stringBuilder.append(' ');
            }
            printWriter.print(Integer.toString(i + 100000).substring(1));
            printWriter.print(
                    " " + stringBuilder + " : " + textifier.text.get(textifier.text.size() - 1));
        }
        for (TryCatchBlockNode tryCatchBlock : method.tryCatchBlocks) {
            tryCatchBlock.accept(traceMethodVisitor);
            printWriter.print(" " + textifier.text.get(textifier.text.size() - 1));
        }
        printWriter.println();
    }

    private static String getUnqualifiedName(final String name) {
        int lastSlashIndex = name.lastIndexOf('/');
        if (lastSlashIndex == -1) {
            return name;
        } else {
            int endIndex = name.length();
            if (name.charAt(endIndex - 1) == ';') {
                endIndex--;
            }
            return name.substring(lastSlashIndex + 1, endIndex);
        }
    }

}
