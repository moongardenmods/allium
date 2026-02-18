package dev.hugeblank.allium.loader.lib.clazz;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface GeneratedFieldValue {
    String DESCRIPTOR = "Lme/hugeblank/allium/util/ClassFieldBuilder$GeneratedFieldValue;";

    String value() default "";

    String description() default "";
}
