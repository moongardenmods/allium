package dev.hugeblank.allium.loader.lib.clazz.definition;

import dev.hugeblank.allium.loader.lib.clazz.builder.ClassBuilder;
import dev.hugeblank.allium.loader.lib.clazz.builder.MethodBuilder;
import org.objectweb.asm.Opcodes;
import org.squiddev.cobalt.function.LuaFunction;

public class ProxyClinitReference extends ExecutableReference {

    private final ClinitReference reference;
    private final String trueIndex;

    public ProxyClinitReference(ClinitReference reference, String index) {
        super("<clinit>", index, new WrappedType[]{}, new WrappedType(MethodBuilder.VOID, MethodBuilder.VOID), Opcodes.ACC_STATIC, true);
        this.reference = reference;
        trueIndex = index;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public String getTypeName() {
        return "class initializer";
    }

    @Override
    public void write(ClassBuilder.BuilderContext bctx) {}

    @Override
    public String index() {
        return trueIndex;
    }

    @Override
    public void setFunction(LuaFunction function) {
        reference.addFunction(function);
    }
}
