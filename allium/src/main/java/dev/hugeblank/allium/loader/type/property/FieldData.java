package dev.hugeblank.allium.loader.type.property;

import dev.hugeblank.allium.loader.type.coercion.TypeCoercions;
import dev.hugeblank.allium.loader.type.exception.InvalidArgumentException;
import me.basiqueevangelist.enhancedreflection.api.EField;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Member;
import java.util.Objects;

public final class FieldData<I> implements PropertyData<I> {
    private final EField field;
    private final VarHandle handle;

    public FieldData(EField field, MemberFilter filter) throws IllegalAccessException {
        this.field = field;
        this.handle = filter.lookup(field.declaringClass().raw()).unreflectVarHandle(field.raw());
    }

    @Override
    public LuaValue get(String name, LuaState state, I instance, boolean noThisArg) throws LuaError {
        return TypeCoercions.toLuaValue(handle.get(instance), field.fieldTypeUse().upperBound());
    }

    @Override
    public void set(String name, LuaState state, I instance, LuaValue value) throws LuaError {
        if (field.isFinal()) {
            PropertyData.super.set(name, state, instance, value);
            return;
        }

        try {
            handle.set(instance, TypeCoercions.toJava(state, value, field.fieldType().upperBound()));
        } catch (InvalidArgumentException e) {
            throw new LuaError(e);
        }
    }
}
