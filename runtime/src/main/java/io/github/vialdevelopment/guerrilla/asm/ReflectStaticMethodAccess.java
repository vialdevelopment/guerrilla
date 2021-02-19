package io.github.vialdevelopment.guerrilla.asm;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.reflect.InvocationTargetException;

import static org.objectweb.asm.Opcodes.*;

/**
 * A small reflect method access, only supports calling statics
 */
public abstract class ReflectStaticMethodAccess {

    public ReflectStaticMethodAccess() {}

    /** Invoke the method pointing to with the specified args */
    abstract public Object invoke(Object... args);

    /**
     * Generates an instance of this pointing to a static method
     * @param parent method's parent class
     * @param name method's name
     * @param desc method's desc
     * @return method access object
     */
    public static ReflectStaticMethodAccess generate(String parent, String name, String desc) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        String newClassName = parent + name + "MethodAccess";

        ClassWriter cw = new ClassWriter(0);
        MethodVisitor mv;
        {
            cw.visit(V1_1, ACC_PUBLIC | ACC_SUPER, newClassName, null, "io/github/vialdevelopment/guerrilla/asm/ReflectStaticMethodAccess", null);
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "io/github/vialdevelopment/guerrilla/asm/ReflectStaticMethodAccess", "<init>", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC | ACC_VARARGS, "invoke", "([Ljava/lang/Object;)Ljava/lang/Object;", null, null);
            mv.visitCode();
            int counter = 0;
            for (Type argumentType : Type.getArgumentTypes(desc)) {
                mv.visitVarInsn(ALOAD, 1);
                mv.visitLdcInsn(counter);
                mv.visitInsn(AALOAD);
                switch(argumentType.getSort()) {
                    case Type.OBJECT:
                    case Type.ARRAY:
                        mv.visitTypeInsn(CHECKCAST, argumentType.getInternalName());
                        break;
                    case Type.BOOLEAN:
                        mv.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
                        break;
                    case Type.BYTE:
                        mv.visitTypeInsn(CHECKCAST, "java/lang/Byte");
                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
                        break;
                    case Type.CHAR:
                        mv.visitTypeInsn(CHECKCAST, "java/lang/Character");
                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
                        break;
                    case Type.SHORT:
                        mv.visitTypeInsn(CHECKCAST, "java/lang/Short");
                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
                        break;
                    case Type.INT:
                        mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
                        break;
                    case Type.FLOAT:
                        mv.visitTypeInsn(CHECKCAST, "java/lang/Float");
                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
                        break;
                    case Type.LONG:
                        mv.visitTypeInsn(CHECKCAST, "java/lang/Long");
                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
                        break;
                    case Type.DOUBLE:
                        mv.visitTypeInsn(CHECKCAST, "java/lang/Double");
                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
                        break;
                }
                counter++;
            }
            mv.visitMethodInsn(INVOKESTATIC, parent, name, desc, false);
            Type returnType = Type.getReturnType(desc);
            switch(returnType.getSort()) {
                case Type.VOID:
                    mv.visitInsn(ACONST_NULL);
                    break;
                case Type.BOOLEAN:
                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
                    break;
                case Type.BYTE:
                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
                    break;
                case Type.CHAR:
                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
                    break;
                case Type.SHORT:
                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
                    break;
                case Type.INT:
                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                    break;
                case Type.FLOAT:
                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
                    break;
                case Type.LONG:
                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
                    break;
                case Type.DOUBLE:
                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                    break;
            }
            mv.visitInsn(ARETURN);
            mv.visitMaxs(Type.getArgumentsAndReturnSizes(desc), Type.getArgumentsAndReturnSizes(desc));
            mv.visitEnd();
        }
        cw.visitEnd();
        return (ReflectStaticMethodAccess) ReflectClassLoader.INSTANCE.define((newClassName).replace('/', '.'), cw.toByteArray()).getConstructor().newInstance();
    }

}
