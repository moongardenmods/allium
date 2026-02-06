package dev.hugeblank.allium.loader.lib.builder.definition;

import dev.hugeblank.allium.loader.lib.builder.ClassBuilder;

public class ConstructorDefinition extends MethodDefinition {

    private final ClassBuilder.ConstructorHandler handler;

    public ConstructorDefinition(ClassBuilder.ConstructorHandler handler, WrappedType[] params, int access, boolean definesFields) {
        super("<init>", params, new WrappedType(ClassBuilder.VOID, ClassBuilder.VOID), access, definesFields);
        this.handler = handler;
    }

    public ClassBuilder.ConstructorHandler handler() {
        return handler;
    }
}
