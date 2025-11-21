package dev.hugeblank.allium.loader.mixin;

import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.squiddev.cobalt.LuaError;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MixinParameter {
    private Type type;
    private final List<LuaAnnotation> annotations;

    public MixinParameter(Type type, @Nullable List<LuaAnnotation> annotations) {
        this.type = type;
        this.annotations = annotations != null ? new ArrayList<>(annotations) : new ArrayList<>();
    }

    public MixinParameter(Type type) {
        this(type, null);
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public void addAnnotation(LuaAnnotation annotation) {
        annotations.add(annotation);
    }

    public void annotate(MethodVisitor methodVisitor, int index) throws LuaError {
        for (LuaAnnotation annotation : annotations) {
            AnnotationVisitor avisitor = attachParameterAnnotation(methodVisitor, index, annotation.type().raw());
            annotation.apply(avisitor);
            avisitor.visitEnd();
        }
    }

    private static AnnotationVisitor attachParameterAnnotation(
            MethodVisitor visitor,
            int index,
            @SuppressWarnings("SameParameterValue") Class<?> annotation
    ) {
        EClass<?> eAnnotation = EClass.fromJava(annotation);
        return visitor.visitParameterAnnotation(index,
                annotation.descriptorString(),
                !eAnnotation.hasAnnotation(Retention.class) ||
                        eAnnotation.annotation(Retention.class).value().equals(RetentionPolicy.RUNTIME)
        );
    }

}
