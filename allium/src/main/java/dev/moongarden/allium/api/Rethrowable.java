package dev.moongarden.allium.api;

import dev.moongarden.allium.loader.type.MethodInvocationFunction;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.Varargs;

///  Exceptions that are thrown by Lua scripts that should not be caught by the MethodInvocationFunction should
/// implement this interface.
///
/// Note that exception implementing this interface MUST be a child of RuntimeException.
///
/// @see MethodInvocationFunction#invoke(LuaState, Varargs)
/// @see RuntimeException
public interface Rethrowable {
}
