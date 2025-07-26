package dev.hugeblank.allium.loader.type;

import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.LuaUserdata;

public class AlliumUserdata extends LuaUserdata {

    public AlliumUserdata(Object obj) {
        super(obj);
    }

    public AlliumUserdata(Object obj, LuaTable metatable) {
        super(obj, metatable);
    }
}

