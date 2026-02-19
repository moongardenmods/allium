package dev.hugeblank.allium.loader.type.property;

import dev.hugeblank.allium.loader.lib.clazz.builder.ClassBuilder;
import dev.hugeblank.allium.loader.type.coercion.TypeCoercions;
import dev.hugeblank.allium.loader.type.exception.InvalidArgumentException;
import me.basiqueevangelist.enhancedreflection.api.EField;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;

import java.lang.invoke.VarHandle;

public final class FieldData<I> implements PropertyData<I> {
    private final EField field;
    private final VarHandle handle;

    public FieldData(EField field, MemberFilter filter) throws IllegalAccessException {
        this.field = field;
        this.handle = filter.lookup(field.declaringClass().raw()).unreflectVarHandle(field.raw());
    }

    @Override
    public LuaValue get(String name, LuaState state, I instance, boolean noThisArg) throws LuaError {
        return TypeCoercions.toLuaValue(field.isStatic() ? handle.get() : handle.get(instance), field.fieldTypeUse().upperBound());
    }

    @Override
    public void set(String name, LuaState state, I instance, LuaValue value) throws LuaError {
        if (field.isFinal()) {
            try {
                Object javaValue = TypeCoercions.toJava(state, value, field.rawFieldType());
                Class<?> declaring = field.declaringClass().raw();
                if (instance == null && ClassBuilder.hasClassFieldHooks(declaring)) {
                    ClassBuilder.setClassFieldHooks(declaring, name, javaValue);
                    return;
                } else if (ClassBuilder.hasInstanceFieldHooks(instance)) {
                    ClassBuilder.setInstanceFieldHooks(instance, name, javaValue);
                    return;
                }
            } catch (InvalidArgumentException e) {
                throw new LuaError(e);
            }
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
