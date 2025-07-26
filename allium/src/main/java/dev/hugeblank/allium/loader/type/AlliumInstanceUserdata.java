package dev.hugeblank.allium.loader.type;

import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.LuaUserdata;

public class AlliumInstanceUserdata<T> extends AlliumUserdata {
    private final EClass<T> clazz;

     AlliumInstanceUserdata(T obj, LuaTable metatable, EClass<T> clazz) {
        super(obj, metatable);
        this.clazz = clazz;
    }

    public EClass<T> instanceClass() {
        return clazz;
    }


    public boolean instanceOf(Class<?> test) {
        return clazz.isAssignableFrom(test);
    }

    public boolean instanceOf(EClass<?> test) {
        return clazz.isAssignableFrom(test);
    }

    public <U> U toUserdata(EClass<U> test) {
        return toUserdata(test.raw());
    }

    public <U> U toUserdata(Class<U> test) {
        return test.cast(instance);
    }

    @Override
    public T toUserdata() {
        return clazz.cast(instance);
    }

    @Override
    public String toString() {
        // TODO: Mapping
        return super.toString() + " [instance of " + clazz.name() + "]";
    }
}
