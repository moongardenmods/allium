package dev.moongarden.allium.loader.lib.clazz.definition;

import dev.moongarden.allium.loader.lib.clazz.builder.ClassBuilder;
import dev.moongarden.allium.loader.lib.clazz.builder.InternalFieldBuilder;
import org.objectweb.asm.MethodVisitor;
import org.squiddev.cobalt.LuaState;

import java.util.List;

public class WriteContext {
    private final MethodVisitor methodVisitor;
    private final String descriptor;
    private final ClassBuilder.BuilderContext classContext;
    private int arrayPos = 0;
    private int paramOffset = 0;

    WriteContext(ClassBuilder.BuilderContext classContext, MethodVisitor m, String descriptor) {
        this.classContext = classContext;
        this.methodVisitor = m;
        this.descriptor = descriptor;
    }

    public MethodVisitor visitor() {
        return methodVisitor;
    }

    public String descriptor() {
        return descriptor;
    }

    public int arrayPos() {
        return arrayPos;
    }

    public void setArrayPos(int i) {
        arrayPos = i;
    }

    public int paramOffset() {
        return paramOffset;
    }

    public void setParamOffset(int i) {
        paramOffset = i;
    }

    public String className() {
        return classContext.className();
    }

    public InternalFieldBuilder fields() {
        return classContext.fields();
    }

    public LuaState state() {
        return classContext.state();
    }

    public List<ClassBuilder.FieldReference> classFields() {
        return classContext.classFields();
    }

    public List<ClassBuilder.FieldReference> instanceFields() {
        return classContext.instanceFields();
    }
}
