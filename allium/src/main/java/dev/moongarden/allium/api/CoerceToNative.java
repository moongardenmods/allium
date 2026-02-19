package dev.moongarden.allium.api;

import java.lang.annotation.*;

/**
 * Annotation that flags that the value represented by this type will be converted to a table when accessed in Lua.
 * Type must be one of {@link java.util.List}, {@link java.util.Set}, or {@link java.util.Map}
 */
@Target(ElementType.TYPE_USE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface
CoerceToNative {
}
