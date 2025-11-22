package dev.hugeblank.allium.util.asm;

import static org.objectweb.asm.Opcodes.ACC_STATIC;

public interface VisitedElement {
    VisitedClass owner();
    int access();
    String name();
    String descriptor();
    String signature();

    default boolean needsInstance() {
        return (access() & ACC_STATIC) == 0;
    }

}
