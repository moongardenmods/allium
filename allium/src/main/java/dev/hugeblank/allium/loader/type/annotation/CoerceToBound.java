package dev.hugeblank.allium.loader.type.annotation;

import java.lang.annotation.*;

/**
 * Annotation that flags for a userdata to cache the class instance that the userdata will be made from.
 * As a result, Lua function invocations that usually would require a `:` to invoke methods on the userdata
 * will only require `.` if this annotation is applied.
 */
@Target(ElementType.TYPE_USE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CoerceToBound {
}
