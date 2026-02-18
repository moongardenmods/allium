package dev.hugeblank.allium.loader.lib.clazz.definition;

import dev.hugeblank.allium.loader.lib.clazz.builder.ClassBuildException;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EConstructor;

public class DefaultSuperConstructorReference extends SuperConstructorReference {
    public DefaultSuperConstructorReference(EClass<?> parent) throws ClassBuildException {
        if (parent.constructor() == null) throw new ClassBuildException(
            "Parent class has no no-arg constructors. This class must have a constructor that calls `super`."
        );
        EConstructor<?> superCtor = parent.constructor();
        super(
            superCtor,
            null,
            null,
            new WrappedType[]{},
            superCtor.modifiers(),
            false
        );
    }

    @Override
    public String index() {
        return null;
    }
}
