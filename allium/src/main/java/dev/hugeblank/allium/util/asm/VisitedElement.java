package dev.hugeblank.allium.util.asm;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.loader.ScriptRegistry;
import dev.hugeblank.allium.mappings.Mappings;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;

import static org.objectweb.asm.Opcodes.ACC_STATIC;

public interface VisitedElement {
    VisitedClass owner();
    int access();
    String name();
    String descriptor();
    String signature();

    default boolean needsInstance() {
        return (access() & ACC_STATIC) == 0;
    }

    default String mappedName(LuaState state) throws LuaError {
        return Allium.DEVELOPMENT ? name() :
                ScriptRegistry.scriptFromState(state).getMappings().getMapped(
                        Mappings.asMethod(owner().name(), name())
                ).split("#")[1];
    }
}
