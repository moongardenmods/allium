package dev.hugeblank.allium.api;

import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.type.userdata.InstanceUserdataFactory;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.function.LibFunction;

/**
 * Class for Libraries which depend on the script responsible for execution.
 *
 * @see WrappedLibrary
 */
public class WrappedScriptLibrary implements WrappedLibrary {
    protected final Script script;

    public WrappedScriptLibrary(Script script) {
        this.script = script;
    }

    public LuaValue add(LuaState state) throws LuaError {
        LuaValue lib = InstanceUserdataFactory.from(EClass.fromJava(getClass())).createBound(this);

        LuaWrapped wrapped = getClass().getAnnotation(LuaWrapped.class);

        if (wrapped == null || wrapped.name().length == 0)
            throw new IllegalStateException("WrappedLibrary must have a @LuaWrapped annotation with a name!");

        for (String name : wrapped.name()) {
            LibFunction.setGlobalLibrary(state, name, lib);
        }

        return lib;
    }
}
