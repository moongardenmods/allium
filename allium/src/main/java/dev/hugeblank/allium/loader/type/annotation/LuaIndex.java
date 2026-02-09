package dev.hugeblank.allium.loader.type.annotation;

import java.lang.annotation.*;

/**
 * Annotation that flags a method to be used in the `__index` invocation on the userdata that represents the class
 * the method is a member of. Should only have one parameter.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface LuaIndex {
}
