package dev.hugeblank.allium.loader.mixin;

import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.objectweb.asm.AnnotationVisitor;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaValue;

public interface Annotating {

    EClass<?> type();

    String name();

    void apply(AnnotationVisitor annotationVisitor) throws LuaError;
}
