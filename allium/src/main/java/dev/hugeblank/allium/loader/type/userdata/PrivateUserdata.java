package dev.hugeblank.allium.loader.type.userdata;

import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EMethod;
import org.jetbrains.annotations.Nullable;
import org.squiddev.cobalt.ErrorFactory;
import org.squiddev.cobalt.LuaTable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class PrivateUserdata<T> extends InstanceUserdata<T> {
    private @Nullable SuperUserdata<? super T> superInstance;

    PrivateUserdata(T obj, LuaTable metatable, EClass<T> clazz) {
        super(obj, metatable, clazz);
    }

    @Override
    public String toString() {
        return ErrorFactory.typeName(this) + ": " + Integer.toHexString(this.hashCode()) +
                " [private instance of " + clazz.name() + "]";
    }

    // Used in ClassBuilder
    @SuppressWarnings("unused")
    public void applySuperInstance(SuperUserdata<? super T> superInstance) {
        if (this.superInstance == null) this.superInstance = superInstance;
    }

    public SuperUserdata<? super T> superInstance() {
        return superInstance;
    }

    @Override
    public Object invoke(EMethod method, Object instance, Object... params) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Class<?> clazz = instanceClass().raw();
        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(clazz, MethodHandles.lookup());
        MethodHandle handle = lookup.findSpecial(clazz, method.name(), MethodType.methodType(method.rawReturnType().raw()), clazz);
        List<Object> paramList = new ArrayList<>(List.of(params));
        paramList.addFirst(instance);
        try {
            return handle.invoke(paramList.toArray());
        } catch (Throwable e) {
            throw new InvocationTargetException(e);
        }
    }
}
