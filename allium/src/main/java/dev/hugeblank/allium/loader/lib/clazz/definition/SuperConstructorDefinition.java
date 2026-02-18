package dev.hugeblank.allium.loader.lib.clazz.definition;

import dev.hugeblank.allium.api.LuaWrapped;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EConstructor;
import me.basiqueevangelist.enhancedreflection.api.EParameter;
import org.objectweb.asm.Type;
import org.squiddev.cobalt.function.LuaFunction;

import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.INVOKESPECIAL;

@LuaWrapped
public class SuperConstructorDefinition extends ConstructorDefinition {
    private final EConstructor<?> superCtor;

    public SuperConstructorDefinition(EConstructor<?> superCtor, LuaFunction remapper, WrappedType[] params, int access, boolean definesFields) {
        super(remapper, params, access, definesFields);
        this.superCtor = superCtor;
    }


    @Override
    public void applyRemap(WriteContext ctx) {
        if (remapper != null) {
            runRemapper(ctx);

            List<Type> remapTypes = new ArrayList<>();
            List<EClass<?>> remapEClasses = new ArrayList<>();
            for (EParameter parameter : superCtor.parameters()) {
                remapTypes.add(Type.getType(parameter.rawParameterType().raw()));
                remapEClasses.add(parameter.rawParameterType());
            }

            applyTypes(ctx, remapTypes, remapEClasses);
        } else {
            loadArguments(ctx.visitor());
        }

        ctx.visitor().visitMethodInsn(
            INVOKESPECIAL,
            Type.getInternalName(superCtor.declaringClass().raw()),
            "<init>",
            Type.getConstructorDescriptor(superCtor.raw()),
            false
        );
    }
}
