package dev.moongarden.allium.loader.type.userdata;

import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.squiddev.cobalt.LuaTable;

public class ArrayUserdata<T> extends InstanceUserdata<T> {
    ArrayUserdata(T obj, LuaTable metatable, EClass<T> clazz) {
        super(obj, metatable, clazz);
    }

    public String toString() {
        return super.toString().replace("[instance of", "[array of"); // this is silly.
    }
}
