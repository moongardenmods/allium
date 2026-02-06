package dev.hugeblank.allium.loader.lib.builder.definition;

public class ExecutableDefinition {

    private final String name;
    private final WrappedType[] params;
    private final WrappedType returns;
    private final int access;
    private final boolean definesFields;

    public ExecutableDefinition(String name, WrappedType[] params, WrappedType returns, int access, boolean definesFields) {
        this.name = name;
        this.params = params;
        this.returns = returns;
        this.access = access;
        this.definesFields = definesFields;
    }

    public String name() {
        return name;
    }

    public WrappedType[] params() {
        return params;
    }

    public WrappedType returns() {
        return returns;
    }

    public int access() {
        return access;
    }

    public boolean definesFields() {
        return definesFields;
    }
}
