package dev.moongarden.allium.loader.type.userdata;

import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.LuaUserdata;

public class ClassUserdata<T> extends LuaUserdata {
    private final EClass<T> clazz;

    public ClassUserdata(EClass<T> clazz, LuaTable metatable) {
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
