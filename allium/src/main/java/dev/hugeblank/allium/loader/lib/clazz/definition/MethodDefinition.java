package dev.hugeblank.allium.loader.lib.clazz.definition;

public class MethodDefinition extends ExecutableDefinition {
    public MethodDefinition(String name, WrappedType[] params, WrappedType returns, int access) {
        super(name, params, returns, access, false);
    }
}
