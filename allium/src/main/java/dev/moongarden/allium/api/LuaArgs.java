package dev.moongarden.allium.api;

import dev.moongarden.allium.loader.lib.JavaLib;
import dev.moongarden.allium.loader.type.MethodInvocationFunction;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.Varargs;

import java.lang.annotation.*;
import java.util.List;

/**
 * Annotation that flags that the remaining arguments of a Lua invocation of this Java method should
 * be put into an instance of {@link org.squiddev.cobalt.Varargs}.
 *
 * @see JavaLib#callWith(LuaState, MethodInvocationFunction, List, Varargs)
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LuaArgs {
}
