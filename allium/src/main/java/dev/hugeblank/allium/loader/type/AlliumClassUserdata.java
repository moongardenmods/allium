package dev.hugeblank.allium.loader.type;

import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.LuaUserdata;

public class AlliumClassUserdata<T> extends LuaUserdata {
    private final EClass<T> clazz;

    public AlliumClassUserdata(EClass<T> clazz, LuaTable metatable) {
        super(clazz, metatable);
        this.clazz = clazz;
    }

    @Override
    public EClass<T> toUserdata() {
        return clazz;
    }

    @Override
    public boolean equals(Object val) {
        if (val instanceof EClass<?>) return clazz.equals(val);
        return false;
    }

    @Override
    public String toString() {
        return super.toString() + " [" + clazz.name() + "]";
    }
}
