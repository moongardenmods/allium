package dev.hugeblank.allium.neoforge.api;

import dev.hugeblank.allium.Allium;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface NeoforgeAlliumExtension {
    Allium.EnvType envType() default Allium.EnvType.COMMON;
}
