package dev.hugeblank.allium.loader.type.userdata;

import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.jetbrains.annotations.Nullable;
import org.squiddev.cobalt.ErrorFactory;
import org.squiddev.cobalt.LuaTable;

public class PrivateUserdata<T> extends SuperUserdata<T> {
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
}
