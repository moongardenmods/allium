package dev.moongarden.allium.loader.lib.clazz.definition;

public class MethodReference extends ExecutableReference {
    public MethodReference(String name, String index, WrappedType[] params, WrappedType returns, int access) {
        super(name, index == null ? name : index, params, returns, access, false);
    }

    @Override
    public String getTypeName() {
        return "method";
    }
}
