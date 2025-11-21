package dev.hugeblank.allium.api;

import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.Varargs;

///  Exceptions that are thrown by Lua scripts that should not be caught by the MethodInvocationFunction should
/// implement this interface.
/// @see dev.hugeblank.allium.loader.type.MethodInvocationFunction#invoke(LuaState, Varargs)
public interface Rethrowable {
}
