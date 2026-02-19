package dev.hugeblank.allium.loader.lib.clazz.builder;

import dev.hugeblank.allium.api.LuaWrapped;
import dev.hugeblank.allium.loader.lib.clazz.definition.ClinitReference;
import dev.hugeblank.allium.loader.lib.clazz.definition.ProxyClinitReference;

public class ClinitBuilder {
    private static ClinitReference reference;

    private final ClassBuilder classBuilder;
    private String index;

    public ClinitBuilder(ClassBuilder classBuilder) {
        this.classBuilder = classBuilder;
    }

    @LuaWrapped
    public ClinitBuilder index(String index) {
        this.index = index;
        return this;
    }

    @LuaWrapped
    public ClassBuilder build() throws ClassBuildException {
        if (reference == null) {
            reference = new ClinitReference(true);
            classBuilder.apply(reference);
        }
        classBuilder.apply(new ProxyClinitReference(reference, index));
        return classBuilder;
    }
}
