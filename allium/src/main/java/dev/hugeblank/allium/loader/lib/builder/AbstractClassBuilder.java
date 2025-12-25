package dev.hugeblank.allium.loader.lib.builder;

import org.objectweb.asm.ClassWriter;

import static org.objectweb.asm.Opcodes.V17;

public class AbstractClassBuilder {
    protected final String superClass;
    protected final String className;
    protected final int access;
    protected final ClassWriter c = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

    public AbstractClassBuilder(String className, String superClass, String[] interfaces, int access, String signature) {
        this.superClass = superClass;
        this.className = className;
        this.access = access;
        this.c.visit(V17, access, className, signature, superClass, interfaces);
    }
}
