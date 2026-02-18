package dev.hugeblank.allium.loader.lib.clazz.builder;

import dev.hugeblank.allium.api.LuaWrapped;
import dev.hugeblank.allium.loader.lib.clazz.definition.ClinitReference;
import dev.hugeblank.allium.loader.lib.clazz.definition.ProxyClinitReference;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;

import java.util.function.Supplier;

public class ClinitBuilder {
    private static ClinitReference reference;

    private final ClassBuilder classBuilder;
    private final LuaState state;
    private final Supplier<LuaValue> hooksSupplier;
    private String index;

    public ClinitBuilder(ClassBuilder classBuilder, LuaState state, Supplier<LuaValue> hooksSupplier) {
        this.classBuilder = classBuilder;
        this.state = state;
        this.hooksSupplier = hooksSupplier;
    }

    @LuaWrapped
    public ClinitBuilder index(String index) {
        this.index = index;
        return this;
    }

    @LuaWrapped
    public ClassBuilder build() throws ClassBuildException {
        if (reference == null) {
            reference = new ClinitReference(state, hooksSupplier);
            classBuilder.apply(reference);
        }
        classBuilder.apply(new ProxyClinitReference(reference, index));
        return classBuilder;
    }
}
