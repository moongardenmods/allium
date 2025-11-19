package dev.hugeblank.allium.util.asm;

import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record VisitedMethod(VisitedClass owner, int access, String name, String descriptor, String signature, String[] exceptions) implements VisitedElement {
    public List<Type> getParams() {
        return Arrays.asList(Type.getArgumentTypes(descriptor));
    }
}
