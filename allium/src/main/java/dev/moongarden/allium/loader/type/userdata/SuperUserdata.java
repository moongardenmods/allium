package dev.moongarden.allium.loader.type.userdata;

import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EMethod;
import org.squiddev.cobalt.ErrorFactory;
import org.squiddev.cobalt.LuaTable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class SuperUserdata<T> extends InstanceUserdata<T> {
    SuperUserdata(T obj, LuaTable metatable, EClass<T> clazz) {
        super(obj, metatable, clazz);
    }

    public EClass<?> superClass() {
        return clazz.superclass();
    }

    @Override
    public String toString() {
        return ErrorFactory.typeName(this) + ": " + Integer.toHexString(this.hashCode()) +
                " [super instance of " + superClass().name() + "]";
    }

    @Override
    public Object invoke(EMethod eMethod, Object instance, Object... params) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Class<?> superClass = clazz.superclass().raw();
        Method method = eMethod.raw();
        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(superClass, MethodHandles.lookup());
        MethodHandle handle = lookup.findSpecial(
                superClass,
                eMethod.name(),
                MethodType.methodType(method.getReturnType(), method.getParameterTypes()),
                clazz.raw()
        );
        List<Object> paramList = new ArrayList<>(List.of(params));
        paramList.addFirst(instance);
        try {
            return handle.invoke(paramList.toArray());
        } catch (Throwable e) {
            throw new InvocationTargetException(e);
        }
    }
}
