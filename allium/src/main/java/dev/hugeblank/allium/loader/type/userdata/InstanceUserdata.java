package dev.hugeblank.allium.loader.type.userdata;

import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EMethod;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.LuaUserdata;

import java.lang.reflect.InvocationTargetException;

public class InstanceUserdata<T> extends LuaUserdata {
    protected final EClass<T> clazz;

     InstanceUserdata(T obj, LuaTable metatable, EClass<T> clazz) {
         super(obj, metatable);
         this.clazz = clazz;
    }

    public EClass<T> instanceClass() {
        return clazz;
    }

    // TODO: We should probably override get/setmetatable since we reuse one for each class/bound (and it's script indiscriminate!)

    public boolean instanceOf(Class<?> test) {
        return clazz.isAssignableFrom(test);
    }

    public boolean instanceOf(EClass<?> test) {
        return clazz.isAssignableFrom(test);
    }

    public <U> U toUserdata(EClass<U> test) {
        return toUserdata(test.raw());
    }

    public <U> U toUserdata(Class<? super U> test) {
        //noinspection unchecked
        return (U) test.cast(instance);
    }

    @Override
    public T toUserdata() {
        return clazz.cast(instance);
    }

    @Override
    public String toString() {
        return super.toString() + " [instance of " + clazz.name() + "]";
    }

    public Object invoke(EMethod eMethod, Object instance, Object... params) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        eMethod.raw().setAccessible(true);
        return eMethod.invoke(instance, params);
    }
}
