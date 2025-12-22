package dev.hugeblank.allium.loader.type.property;

import dev.hugeblank.allium.loader.type.coercion.TypeCoercions;
import dev.hugeblank.allium.loader.type.exception.InvalidArgumentException;
import me.basiqueevangelist.enhancedreflection.api.EField;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class InternalFieldData<I> implements PropertyData<I> {
    private final EField field;
    private final VarHandle fieldHandle;

    public InternalFieldData(EField field) throws IllegalAccessException {
        this.field = field;
        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(field.declaringClass().raw(), MethodHandles.lookup());
        fieldHandle = lookup.unreflectVarHandle(field.raw());
    }

    @Override
    public LuaValue get(String name, LuaState state, I instance, boolean noThisArg) throws LuaError {
        return TypeCoercions.toLuaValue(fieldHandle.get(instance), field.fieldTypeUse().upperBound());
    }

    @Override
    public void set(String name, LuaState state, I instance, LuaValue value) throws LuaError {
        if (field.isFinal()) {
            PropertyData.super.set(name, state, instance, value);
            return;
        }

        try {
            fieldHandle.set(instance, TypeCoercions.toJava(state, value, field.fieldType().upperBound()));
        } catch (InvalidArgumentException e) {
            throw new LuaError(e);
        }
    }
}
