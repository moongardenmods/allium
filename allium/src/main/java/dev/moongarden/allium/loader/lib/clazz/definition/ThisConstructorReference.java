package dev.moongarden.allium.loader.lib.clazz.definition;

import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.objectweb.asm.Type;
import org.squiddev.cobalt.function.LuaFunction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.objectweb.asm.Opcodes.INVOKESPECIAL;

public class ThisConstructorReference extends ConstructorReference {
    private final ConstructorReference thisDef;
    private final String className;

    public ThisConstructorReference(ConstructorReference thisDef, LuaFunction remapper, String index, String className, WrappedType[] params, int access, boolean definesFields) {
        super(remapper, index, params, access, definesFields);
        this.thisDef = thisDef;
        this.className = className;
    }

    @Override
    public void applyRemap(WriteContext ctx) {
        if (remapper != null) {
            runRemapper(ctx);

            List<Type> remapTypes = new ArrayList<>();
            List<EClass<?>> remapEClasses = new ArrayList<>();
            for (WrappedType parameter : thisDef.params()) {
                remapTypes.add(Type.getType(parameter.raw().raw()));
                remapEClasses.add(parameter.raw());
            }

            applyTypes(ctx, remapTypes, remapEClasses);
        } else {
            loadArguments(ctx.visitor());
        }

        Type[] paramsType = Arrays.stream(thisDef.params())
            .map((wt) -> Type.getType(wt.raw().raw()))
            .toArray(Type[]::new);

        ctx.visitor().visitMethodInsn(
            INVOKESPECIAL,
            "L" + className + ";",
            "<init>",
            Type.getMethodDescriptor(Type.getType(void.class), paramsType),
            false
        );
    }
}
