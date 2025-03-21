package dev.hugeblank.allium.util.asm;

import dev.hugeblank.allium.loader.ScriptRegistry;
import dev.hugeblank.allium.mappings.Mappings;
import org.objectweb.asm.*;
import org.spongepowered.asm.service.MixinService;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.objectweb.asm.Opcodes.ACC_INTERFACE;
import static org.objectweb.asm.Opcodes.ASM9;

public class VisitedClass {
    private static final Map<String, VisitedClass> VISITED = new HashMap<>();

    private final Map<String, VisitedField> visitedFields = new HashMap<>();
    private final Map<String, VisitedMethod> visitedMethods = new HashMap<>();

    private final String mappedClassName;
    private final int version;
    private final int access;
    private final String className;
    private final String signature;
    private final String superName;
    private final String[] interfaces;

    public VisitedClass(String mappedClassName, int version, int access, String className, String signature, String superName, String[] interfaces) {
        this.mappedClassName = mappedClassName;
        this.version = version;
        this.access = access;
        this.className = className;
        this.signature = signature;
        this.superName = superName;
        this.interfaces = interfaces;
        VISITED.put(mappedClassName, this);
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

    private void addVisitedField(LuaState state, int access, String name, String descriptor, String signature, Object value) throws LuaError {
        String[] mapped = ScriptRegistry.scriptFromState(state).getMappings().getMapped(Mappings.asMethod(className, name)).split("#");
        visitedFields.put(mapped[1], new VisitedField(this, access, name, descriptor, signature, value));
    }

    private void addVisitedMethod(LuaState state, int access, String name, String descriptor, String signature, String[] exceptions) throws LuaError {
        String[] mapped = ScriptRegistry.scriptFromState(state).getMappings().getMapped(Mappings.asMethod(className, name)).split("#");
        String key = mapped[1];
        StringBuilder mappedDescriptor = new StringBuilder("(");
        for (Type arg : Type.getArgumentTypes(descriptor)) {
            mapTypeArg(state, mappedDescriptor, arg);
        }
        mappedDescriptor.append(")");
        mapTypeArg(state, mappedDescriptor, Type.getReturnType(descriptor));
        if (!name.equals("<init>") && !name.equals("<clinit>")) {
            key = key+mappedDescriptor;
        }
        visitedMethods.put(key, new VisitedMethod(this, access, name, descriptor, signature, exceptions));
    }

    private void mapTypeArg(LuaState state, StringBuilder mappedDescriptor, Type arg) throws LuaError {
        if (arg.getSort() == Type.OBJECT) {
            mappedDescriptor
                    .append("L")
                    .append(ScriptRegistry.scriptFromState(state).getMappings().getUnmapped(arg.getInternalName()).get(0))
                    .append(";");
        } else {
            mappedDescriptor.append(arg.getInternalName());
        }
    }

    public String mappedClassName() {
        return mappedClassName;
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

    public static VisitedClass ofClass(LuaState state, String mappedClassName) throws LuaError {
        if (!VISITED.containsKey(mappedClassName)) {
            String unmappedName = ScriptRegistry.scriptFromState(state).getMappings().getUnmapped(mappedClassName).get(0);
            try {
                final AtomicReference<LuaError> err = new AtomicReference<>(null);
                new ClassReader(MixinService.getService().getResourceAsStream(unmappedName.replace(".", "/") + ".class")).accept(
                        new ClassVisitor(ASM9) {
                            VisitedClass instance;

                            @Override
                            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                                instance = new VisitedClass(mappedClassName, version, access, name, signature, superName, interfaces);
                                super.visit(version, access, name, signature, superName, interfaces);
                            }

                            @Override
                            public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                                if (err.get() == null) {
                                    try {
                                        instance.addVisitedField(state, access, name, descriptor, signature, value);
                                    } catch (LuaError e) {
                                        err.set(e);
                                    }
                                }
                                return super.visitField(access, name, descriptor, signature, value);
                            }

                            @Override
                            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                                if (err.get() == null) {
                                    try {
                                        instance.addVisitedMethod(state, access, name, descriptor, signature, exceptions);
                                    } catch (LuaError e) {
                                        err.set(e);
                                    }
                                }
                                return super.visitMethod(access, name, descriptor, signature, exceptions);
                            }
                        },
                        ClassReader.SKIP_FRAMES
                );
                if (err.get() != null) throw err.get();
            } catch (IOException e) {
                throw new LuaError(new RuntimeException("Could not read target class: " + mappedClassName + " (unmapped:" + unmappedName + ")", e));
            }
        }
        return VISITED.get(mappedClassName);
    }

    public static void clear() {
        VISITED.clear();
    }
}
