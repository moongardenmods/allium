package dev.hugeblank.allium.loader.type.annotation;

import java.lang.annotation.*;

/**
 * Annotation that flags that this parameter is not required to be present in Lua invocations of the method this
 * parameter is a part of.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@Documented
public @interface OptionalArg {
}
