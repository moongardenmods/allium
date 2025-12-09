package dev.hugeblank.allium.loader.type.builder;

import dev.hugeblank.allium.util.Pair;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.function.Function;

import static org.objectweb.asm.Opcodes.*;

public class FieldBuilder extends AbstractFieldBuilder{
    private int fieldIndex = 0;
    private final HashMap<String, Pair<Object, Class<?>>> storedFields = new HashMap<>();
    private final HashMap<String, Function<Class<?>, ?>> complexFields = new HashMap<>();

    public FieldBuilder(String className, ClassVisitor c) {
        super(className, c);
    }

    public <T> String store(T o, Class<T> fieldType) {
        for (var entry : storedFields.entrySet()) {
            if (o == entry.getValue().left() && fieldType.isAssignableFrom(entry.getValue().right())) {
                return entry.getKey();
            }
        }

        String fieldName = "allium$field" + fieldIndex++;

        var f = c.visitField(ACC_PUBLIC | ACC_STATIC, fieldName, Type.getDescriptor(fieldType), null, null);
        var a = f.visitAnnotation(GeneratedFieldValue.DESCRIPTOR, true);
        a.visit("value", o.toString());
        a.visitEnd();

        storedFields.put(fieldName, new Pair<>(o, fieldType));
        return fieldName;
    }

    public <T> String storeComplex(Function<Class<?>, T> supplier, Class<T> fieldType, String description) {
        String fieldName = "allium$field" + fieldIndex++;

        var f = c.visitField(ACC_PUBLIC | ACC_STATIC, fieldName, Type.getDescriptor(fieldType), null, null);
        var a = f.visitAnnotation(GeneratedFieldValue.DESCRIPTOR, true);
        a.visit("description", description);
        a.visitEnd();

        complexFields.put(fieldName, supplier);
        return fieldName;
    }

    public <T> void storeAndGet(MethodVisitor m, T o, Class<T> type) {
        m.visitFieldInsn(GETSTATIC, className, store(o, type), Type.getDescriptor(type));
    }

    public <T> void storeAndGetComplex(MethodVisitor m, Function<Class<?>, T> supplier, Class<T> type, String description) {
        m.visitFieldInsn(GETSTATIC, className, storeComplex(supplier, type, description), Type.getDescriptor(type));
    }

    public void apply(Class<?> builtClass) {
        try {
            for (var entry : storedFields.entrySet()) {
                builtClass.getField(entry.getKey()).set(null, entry.getValue().left());
            }

            for (var entry : complexFields.entrySet()) {
                builtClass.getField(entry.getKey()).set(null, entry.getValue().apply(builtClass));
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to apply fields to class", e);
        }
    }

}
