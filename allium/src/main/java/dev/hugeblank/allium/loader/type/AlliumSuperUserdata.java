package dev.hugeblank.allium.loader.type;

import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.squiddev.cobalt.LuaTable;

public class AlliumSuperUserdata<T> extends AlliumInstanceUserdata<T> {
    AlliumSuperUserdata(T obj, LuaTable metatable, EClass<T> clazz) {
        super(obj, metatable, clazz);
    }

    public EClass<?> superClass() {
        return clazz.superclass();
    }
}
