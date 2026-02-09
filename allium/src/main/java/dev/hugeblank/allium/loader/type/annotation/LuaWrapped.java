package dev.hugeblank.allium.loader.type.annotation;

import java.lang.annotation.*;

/**
 * Annotation that, when applied to a class, flags that methods, fields, and constructors should be hidden on the Lua
 * userdata, unless they are also annotated with this annotation.
 * Independent of that, if a method or field is annotated with this annotation, and the `name` array has elements, the
 * key with which to index it in Lua changes to the provided names.
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface LuaWrapped {
    String[] name() default {};

    int priority() default 1000;
}
