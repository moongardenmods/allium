package dev.moongarden.allium.api;

import java.lang.annotation.*;

/**
 * Annotation that flags that the value represented by this type will be converted to a table when accessed in Lua.
 * Type must inherit from one of {@link java.util.Collection}, or {@link java.util.Map}
 */
@Target(ElementType.TYPE_USE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface
CoerceToNative {
}
