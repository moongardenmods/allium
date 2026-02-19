package dev.moongarden.allium.loader.lib.clazz.definition;

import dev.moongarden.allium.loader.lib.clazz.builder.ClassBuilder;
import dev.moongarden.allium.loader.lib.clazz.builder.MethodBuilder;
import org.objectweb.asm.Opcodes;
import org.squiddev.cobalt.function.LuaFunction;

public class ProxyClinitReference extends ExecutableReference {

    private final ClinitReference reference;

    public ProxyClinitReference(ClinitReference reference, String index) {
        super("<clinit>", index == null ? "initializer" : index, new WrappedType[]{}, new WrappedType(MethodBuilder.VOID, MethodBuilder.VOID), Opcodes.ACC_STATIC, true);
        this.reference = reference;
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
    public void setFunction(LuaFunction function) {
        reference.addFunction(function);
    }
}
