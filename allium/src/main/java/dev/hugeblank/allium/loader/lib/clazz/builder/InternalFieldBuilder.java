package dev.hugeblank.allium.loader.lib.clazz.builder;

import dev.hugeblank.allium.loader.lib.clazz.GeneratedFieldValue;
import dev.hugeblank.allium.util.Pair;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.HashMap;
import java.util.function.Function;

import static org.objectweb.asm.Opcodes.*;

public class InternalFieldBuilder {
    private final String className;
    private final ClassVisitor c;
    private int fieldIndex = 0;
    private final HashMap<String, Pair<Object, Class<?>>> storedFields = new HashMap<>();
    private final HashMap<String, Pair<Function<Class<?>, ?>, Class<?>>> complexFields = new HashMap<>();

    public InternalFieldBuilder(String className, ClassVisitor c) {
        this.className = className;
        this.c = c;
    }

    public <T> String store(T o, Class<T> fieldType) {
        for (var entry : storedFields.entrySet()) {
            if (o == entry.getValue().left() && fieldType.isAssignableFrom(entry.getValue().right())) {
                return entry.getKey();
            }
        }

        String fieldName = "allium_private$field" + fieldIndex++;

        var f = c.visitField(ACC_PRIVATE | ACC_STATIC, fieldName, Type.getDescriptor(fieldType), null, null);
        var a = f.visitAnnotation(GeneratedFieldValue.DESCRIPTOR, true);
        a.visit("value", o.toString());
        a.visitEnd();

        storedFields.put(fieldName, new Pair<>(o, fieldType));
        return fieldName;
    }

    public <T> String storeComplex(Function<Class<?>, T> supplier, Class<T> fieldType, String description) {
        for (var entry : complexFields.entrySet()) {
            if (supplier == entry.getValue().left() && fieldType.equals(entry.getValue().right())) {
                return entry.getKey();
            }
        }

        String fieldName = "allium_private$field" + fieldIndex++;

        var f = c.visitField(ACC_PRIVATE | ACC_STATIC, fieldName, Type.getDescriptor(fieldType), null, null);
        var a = f.visitAnnotation(GeneratedFieldValue.DESCRIPTOR, true);
        a.visit("description", description);
        a.visitEnd();

        complexFields.put(fieldName, new Pair<>(supplier, fieldType));
        return fieldName;
    }

    public <T> String storeAndGet(MethodVisitor m, T o, Class<T> type) {
        String field = store(o, type);
        m.visitFieldInsn(GETSTATIC, className, field, Type.getDescriptor(type));
        return field;
    }

    public <T> String storeAndGetComplex(MethodVisitor m, Function<Class<?>, T> supplier, Class<T> type, String description) {
        String field = storeComplex(supplier, type, description);
        m.visitFieldInsn(GETSTATIC, className, field, Type.getDescriptor(type));
        return field;
    }

    public void get(MethodVisitor m, String fieldName) {
        Class<?> type;
        if (storedFields.containsKey(fieldName)) {
            type = storedFields.get(fieldName).right();
        } else if (complexFields.containsKey(fieldName)) {
            type = complexFields.get(fieldName).right();
        } else {
            throw new RuntimeException("Failed to get type of " + fieldName);
        }
        m.visitFieldInsn(GETSTATIC, className, fieldName, Type.getDescriptor(type));
    }

    public void apply(Class<?> builtClass) {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(builtClass, MethodHandles.lookup());
            for (var entry : storedFields.entrySet()) {
                VarHandle var = lookup.findStaticVarHandle(builtClass, entry.getKey(), entry.getValue().right());
                var.set(entry.getValue().left());
            }

            for (var entry : complexFields.entrySet()) {
                Object value = entry.getValue().left().apply(builtClass);
                VarHandle var = lookup.findStaticVarHandle(builtClass, entry.getKey(), entry.getValue().right());
                var.set(value);
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to apply fields to class", e);
        }
    }

}
