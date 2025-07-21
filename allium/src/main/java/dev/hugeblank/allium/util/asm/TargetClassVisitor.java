package dev.hugeblank.allium.util.asm;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.loader.ScriptRegistry;
import dev.hugeblank.allium.mappings.Mappings;
import dev.hugeblank.allium.mappings.NoSuchMappingException;
import org.objectweb.asm.*;
import org.spongepowered.asm.service.MixinService;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.objectweb.asm.Opcodes.ASM9;

public class TargetClassVisitor extends ClassVisitor {
    private final LuaState state;
    private final String mappedName;
    private VisitedClass instance;
    private final AtomicReference<LuaError> err = new AtomicReference<>();

    protected TargetClassVisitor(LuaState state, String mappedName, int api) {
        super(api);
        this.state = state;
        this.mappedName = mappedName;
    }

    public LuaError getError() {
        return err.get();
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        instance = new VisitedClass(new MappingsPair(name, mappedName), version, access, signature, superName, interfaces);
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

    public static VisitedClass parseTarget(LuaState state, String mappedTarget, String unmappedTarget) throws IOException, LuaError {
        TargetClassVisitor visitor = new TargetClassVisitor(state, mappedTarget, ASM9);
        new ClassReader(MixinService.getService().getResourceAsStream(
                unmappedTarget + ".class")
        ).accept(
                visitor, ClassReader.SKIP_FRAMES
        );
        List<String> inheritanceNames = new ArrayList<>();
        if (visitor.instance.superName() != null) inheritanceNames.add(visitor.instance.superName());
        inheritanceNames.addAll(List.of(visitor.instance.interfaces()));
        for (String name : inheritanceNames) {
            visitor.instance.addInheritance(parseUnmappedTarget(state, name));
        }
        if (visitor.getError() != null) throw visitor.getError();
        return visitor.instance;
    }

    public static VisitedClass parseUnmappedTarget(LuaState state, String unmappedTarget) throws LuaError, IOException {
        String mappedTarget;
        if (!Allium.DEVELOPMENT) {
            try {
                mappedTarget = ScriptRegistry.scriptFromState(state).getMappings().toMappedClassName(unmappedTarget);
            } catch (NoSuchMappingException ignored) {
                mappedTarget = unmappedTarget;
            }
        } else {
            mappedTarget = unmappedTarget;
        }
        return parseTarget(state, mappedTarget, unmappedTarget);
    }

    public static VisitedClass parseMappedTarget(LuaState state, String mappedTarget) throws LuaError, IOException {
        String unmappedTarget;
        if (!Allium.DEVELOPMENT) {
            try {
                unmappedTarget = ScriptRegistry.scriptFromState(state).getMappings().toUnmappedClassName(Mappings.toSlashedClasspath(mappedTarget));
            } catch (NoSuchMappingException e) {
                unmappedTarget = mappedTarget; // TODO: Warn that mapping couldn't be found
            }
        } else {
            unmappedTarget = mappedTarget;
        }
        return parseTarget(state, mappedTarget, unmappedTarget);
    }

    public record MappingsPair(String unmapped, String mapped) {
        // TODO: Make a class, replace in constructor
        public String unmappedPath() {
            return unmapped.replace(".", "/");
        }

        public String mappedPath() {
            return mapped.replace(".", "/");
        }

    }
}
