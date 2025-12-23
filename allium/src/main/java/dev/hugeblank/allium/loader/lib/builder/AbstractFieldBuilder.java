package dev.hugeblank.allium.loader.lib.builder;

import org.objectweb.asm.ClassVisitor;

public class AbstractFieldBuilder {
    protected final String className;
    protected final ClassVisitor c;

    public AbstractFieldBuilder(String className, ClassVisitor c) {
        this.className = className;
        this.c = c;
    }
}
