package dev.hugeblank.allium.api.annotation;

import java.lang.annotation.*;

/**
 * Annotation that flags that this parameter should be the {@link org.squiddev.cobalt.LuaState} belonging to the
 * {@link dev.hugeblank.allium.loader.Script} calling the method that this parameter belongs to. As a result, on the Lua
 * side of the invocation of this method, the parameter should be ignored as it is handled internally.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LuaStateArg {
}
