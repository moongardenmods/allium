package dev.moongarden.allium.loader.lib.mixin.annotation;

import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.objectweb.asm.AnnotationVisitor;
import org.squiddev.cobalt.LuaError;

public interface Annotating {

    EClass<?> type();

    String name();

    void apply(AnnotationVisitor annotationVisitor) throws LuaError;
}
