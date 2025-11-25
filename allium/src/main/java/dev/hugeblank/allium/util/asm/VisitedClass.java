package dev.hugeblank.allium.util.asm;

import org.objectweb.asm.*;
import org.spongepowered.asm.service.MixinService;
import org.squiddev.cobalt.LuaError;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.objectweb.asm.Opcodes.ACC_INTERFACE;
import static org.objectweb.asm.Opcodes.ASM9;

public class VisitedClass {
    private static final Map<String, VisitedClass> VISITED = new HashMap<>();

    private final Map<String, VisitedField> visitedFields = new HashMap<>();
    private final Map<String, VisitedMethod> visitedMethods = new HashMap<>();

    private final int version;
    private final int access;
    private final String className;
    private final String signature;
    private final String superName;
    private final String[] interfaces;

    public VisitedClass(int version, int access, String className, String signature, String superName, String[] interfaces) {
        this.version = version;
        this.access = access;
        this.className = className;
        this.signature = signature;
        this.superName = superName;
        this.interfaces = interfaces;
        VISITED.put(className.replace("/", "."), this);
    }

    public Type getType() {
        return Type.getType("L"+className+";");
    }

    public boolean containsMethod(String name) {
        return visitedMethods.containsKey(name);
    }

    public VisitedMethod getMethod(String name) {
        return visitedMethods.get(name);
    }

    public boolean containsField(String name) {
        return visitedFields.containsKey(name);
    }

    public VisitedField getField(String name) {
        return visitedFields.get(name);
    }

    public VisitedElement get(String name) {
        if (visitedMethods.containsKey(name)) {
            return visitedMethods.get(name);
        } else {
            return visitedFields.get(name);
        }
    }

    private void addVisitedField(int access, String name, String descriptor, String signature, Object value) {
        visitedFields.put(name, new VisitedField(this, access, name, descriptor, signature, value));
    }

    private void addVisitedMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        visitedMethods.put(name+descriptor, new VisitedMethod(this, access, name, descriptor, signature, exceptions));
    }

    public boolean isInterface() {
        return (access & ACC_INTERFACE) != 0;
    }

    public int version() {
        return version;
    }

    public int access() {
        return access;
    }

    public String name() {
        return className;
    }

    public String signature() {
        return signature;
    }

    public String superName() {
        return superName;
    }

    public String[] interfaces() {
        return interfaces;
    }

    public static VisitedClass ofClass(String className) throws LuaError {
        if (!VISITED.containsKey(className)) {
            try {
                final AtomicReference<LuaError> err = new AtomicReference<>(null);
                InputStream classInputStream = MixinService.getService().getResourceAsStream(className.replace(".", "/") + ".class");
                new ClassReader(classInputStream).accept(
                        new ClassVisitor(ASM9) {
                            VisitedClass instance;

                            @Override
                            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                                instance = new VisitedClass(version, access, name, signature, superName, interfaces);
                                super.visit(version, access, name, signature, superName, interfaces);
                            }

                            @Override
                            public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                                if (err.get() == null) {
                                    instance.addVisitedField(access, name, descriptor, signature, value);
                                }
                                return super.visitField(access, name, descriptor, signature, value);
                            }

                            @Override
                            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                                if (err.get() == null) {
                                    instance.addVisitedMethod(access, name, descriptor, signature, exceptions);
                                }
                                return super.visitMethod(access, name, descriptor, signature, exceptions);
                            }
                        },
                        ClassReader.SKIP_FRAMES
                );
                if (err.get() != null) throw err.get();
            } catch (IOException e) {
                throw new LuaError(new RuntimeException("Could not read target class: " + className, e));
            }
        }
        return VISITED.get(className);
    }

    public static void clear() {
        VISITED.clear();
    }
}
