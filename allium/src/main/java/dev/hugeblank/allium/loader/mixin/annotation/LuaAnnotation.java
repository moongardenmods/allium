package dev.hugeblank.allium.loader.mixin.annotation;

import dev.hugeblank.allium.loader.type.exception.InvalidArgumentException;
import dev.hugeblank.allium.loader.type.coercion.TypeCoercions;
import me.basiqueevangelist.enhancedreflection.api.ClassType;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EMethod;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.squiddev.cobalt.*;

import java.util.*;
import java.util.function.Consumer;

public class LuaAnnotation implements Annotating {

    private final LuaState state;
    private final EClass<?> clazz;
    private final String name;
    private final List<Annotating> prototype = new ArrayList<>();

    public LuaAnnotation(LuaState state, @Nullable String name, LuaTable input, EClass<?> annotationClass) throws InvalidArgumentException, LuaError {
        this.state = state;
        this.clazz = annotationClass;
        this.name = name;
        for (EMethod method : annotationClass.declaredMethods()) {
            EClass<?> returnType = method.rawReturnType();
            EClass<?> arrayComponent = returnType.arrayComponent();
            boolean required = method.raw().getDefaultValue() == null;
            LuaValue value = input.rawget(method.name());
            if (method.name().equals("value") && value.isNil()) {
                value = input.rawget(1);
            }
            if (required && value.isNil()) throw new LuaError("Expected value for '" + name + "' in annotation class " + annotationClass.name());
            if (!value.isNil()) {
                if (arrayComponent == null) {
                    createAnnotating(method.name(), value, returnType, prototype::add);
                } else {
                    LuaTable tvalue = value.checkTable();
                    List<Annotating> elements = new ArrayList<>();
                    for (int i = 1; i <= tvalue.size(); i++) {
                        createAnnotating(null, tvalue.rawget(i), arrayComponent, elements::add);
                    }
                    prototype.add(new ArrayElement(returnType, method.name(), elements));
                }
            }
        }
    }

    private void createAnnotating(@Nullable String key, LuaValue value, EClass<?> returnType, Consumer<Annotating> consumer) throws LuaError, InvalidArgumentException {
        if (returnType.type() == ClassType.ANNOTATION) {
            if (value.type() != Constants.TTABLE) throw new LuaError("Expected table while annotating type " + returnType.name());
            consumer.accept(new LuaAnnotation(state, key, value.checkTable(), returnType));
        } else if (returnType.raw().isEnum() && value.isString()) {
            consumer.accept(new EnumElement(returnType, key, value.checkString()));
        } else if (!value.isNil()) {
            consumer.accept(new Element(returnType, key, TypeCoercions.toJava(state, value, returnType)));
        }
    }

    public LuaAnnotation(LuaState state, LuaTable input, EClass<?> annotationClass) throws InvalidArgumentException, LuaError {
        this(state, null, input, annotationClass);
    }

    public String name() {
        return name;
    }

    @Override
    public EClass<?> type() {
        return clazz;
    }

    @Override
    public void apply(AnnotationVisitor visitor) throws LuaError {
        for (Annotating value : prototype) {
            if (value instanceof LuaAnnotation annotation) {
                AnnotationVisitor nextVisitor = visitor.visitAnnotation(annotation.name(), value.type().raw().descriptorString());
                annotation.apply(nextVisitor);
                nextVisitor.visitEnd();
            } else {
                value.apply(visitor);
            }
        }
    }

    public <T> T findElement(String name, EClass<T> eClass) throws LuaError {
        for (Annotating annotating : prototype) {
            if (annotating.name().equals(name)) {
                try {
                    if (annotating.type().equals(eClass) && annotating instanceof Element element) {
                        return eClass.cast(element.object());
                    } else if (annotating.type().arrayComponent().equals(eClass) && annotating instanceof ArrayElement arrayElement) {
                        List<Annotating> objects = arrayElement.objects();
                        if (!objects.isEmpty() && objects.get(0) instanceof Element element) {
                            return eClass.cast(element.object());
                        } else {
                            throw new LuaError("Annotating element '" + name + "' is empty");
                        }
                    }
                } catch (ClassCastException e) {
                    throw new LuaError("Annotating element '" + name + "' is not of type " + eClass.name());
                }
            }
        }
        return null;
    }

    public <T> T findElement(String name, Class<T> clazz) throws LuaError {
        return findElement(name, EClass.fromJava(clazz));
    }

    private record Element(EClass<?> type, String name, Object object) implements Annotating {

        @Override
        public void apply(AnnotationVisitor annotationVisitor) throws LuaError {
            if (object == null) throw new LuaError("Missing value for key '" + name + "'");
            annotationVisitor.visit(name, object);
        }
    }

    private record EnumElement(EClass<?> type, String name, String value) implements Annotating {

        @Override
        public void apply(AnnotationVisitor annotationVisitor) {
            annotationVisitor.visitEnum(name, type.raw().descriptorString(), value);
        }
    }

    private record ArrayElement(EClass<?> type, String name, List<Annotating> objects) implements Annotating {

        @Override
        public void apply(AnnotationVisitor annotationVisitor) throws LuaError {
            AnnotationVisitor visitor = annotationVisitor.visitArray(name);
            for (Annotating object : objects) {
                if (object instanceof LuaAnnotation annotation) {
                    AnnotationVisitor nextVisitor = visitor.visitAnnotation(annotation.name(), object.type().raw().descriptorString());
                    annotation.apply(nextVisitor);
                    nextVisitor.visitEnd();
                } else {
                    object.apply(visitor);
                }
            }
            visitor.visitEnd();
        }
    }

}
