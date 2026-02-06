package dev.hugeblank.allium.loader.lib.builder.definition;

public class MethodDefinition extends ExecutableDefinition {
    public MethodDefinition(String name, WrappedType[] params, WrappedType returns, int access, boolean definesFields) {
        super(name, params, returns, access, definesFields);
    }
}
