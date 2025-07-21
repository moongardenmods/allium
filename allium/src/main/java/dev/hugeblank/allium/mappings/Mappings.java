package dev.hugeblank.allium.mappings;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EMember;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.VisitableMappingTree;
import org.jetbrains.annotations.Debug;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@LuaWrapped
@Debug.Renderer(text = "\"Mappings { ... }\"", hasChildren = "false")
public class Mappings {
    private final VisitableMappingTree tree;
    private final int dstID;


    public Mappings(@NotNull VisitableMappingTree tree, String namedNS) {
        this.tree = tree;
        this.dstID = tree.getNamespaceId(namedNS);
    }

    public int getDstID() {
        return dstID;
    }

    @LuaWrapped
    public MappingTree.ClassMapping toUnmappedClass(String mappedName) throws NoSuchMappingException {
        MappingTree.ClassMapping classMapping = tree.getClass(mappedName);
        if (classMapping == null) throw new NoSuchMappingException(tree, mappedName);
        return classMapping;
    }

    @LuaWrapped
    public String toUnmappedClassName(String mappedName) throws NoSuchMappingException {
        MappingTree.ClassMapping classMapping = toUnmappedClass(mappedName);
        String result = classMapping.getDstName(dstID);
        if (result == null) return mappedName;
        return result;
    }

    @LuaWrapped
    public MappingTree.MemberMapping toUnmappedMember(MappingTree.ClassMapping classMapping, String mappedName, String mappedDescriptor) throws NoSuchMappingException {
        MappingTree.MethodMapping methodMapping = classMapping.getMethod(mappedName, mappedDescriptor, dstID);
        MappingTree.FieldMapping fieldMapping = classMapping.getField(mappedName, mappedDescriptor, dstID);
        if (methodMapping == null && fieldMapping == null) throw new NoSuchMappingException(tree, mappedName + mappedDescriptor);
        return methodMapping != null ? methodMapping : fieldMapping;
    }

    @LuaWrapped
    public String toUnmappedMemberName(MappingTree.ClassMapping classMapping, String mappedName, String mappedDescriptor) throws NoSuchMappingException {
        MappingTree.MemberMapping memberMapping = toUnmappedMember(classMapping, mappedName, mappedDescriptor);
        String result = memberMapping.getDstName(dstID);
        if (result == null) return mappedName;
        return result;
    }

    @LuaWrapped
    public MappingTree.ClassMapping toMappedClass(String unmappedName) throws NoSuchMappingException {
        MappingTree.ClassMapping classMapping = tree.getClass(unmappedName, dstID);
        if (classMapping == null) throw new NoSuchMappingException(tree, unmappedName);
        return classMapping;
    }

    @LuaWrapped
    public String toMappedClassName(String unmappedName) throws NoSuchMappingException {
        MappingTree.ClassMapping classMapping = toMappedClass(unmappedName);
        String result = classMapping.getSrcName();
        if (result == null) return unmappedName;
        return result;
    }

    @LuaWrapped
    public MappingTree.MemberMapping toMappedMember(MappingTree.ClassMapping classMapping, String unmappedName, String unmappedDescriptor) throws NoSuchMappingException {
        MappingTree.MethodMapping methodMapping = classMapping.getMethod(unmappedName, unmappedDescriptor);
        MappingTree.FieldMapping fieldMapping = classMapping.getField(unmappedName, unmappedDescriptor);
        if (methodMapping == null && fieldMapping == null) throw new NoSuchMappingException(tree, unmappedName + unmappedDescriptor);
        return methodMapping != null ? methodMapping : fieldMapping;
    }

    @LuaWrapped
    public String toMappedMemberName(MappingTree.ClassMapping classMapping, String unmappedName, String unmappedDescriptor) throws NoSuchMappingException {
        MappingTree.MemberMapping memberMapping = toUnmappedMember(classMapping, unmappedName, unmappedDescriptor);
        String result = memberMapping.getSrcName();
        if (result == null) return unmappedName;
        return result;
    }


    @LuaWrapped
    public static String toDottedMember(String className, String method) {
        return toDottedClasspath(className) + "#" + method;
    }

    @LuaWrapped
    public static String toDottedMember(EClass<?> clazz, EMember member) {
        return toDottedMember(clazz.name(), member.name());
    }

    @LuaWrapped
    public static String toDottedClasspath(String className) {
        return className.replace('/', '.');
    }

    @LuaWrapped
    public static String toDottedClasspath(EClass<?> clazz) {
        return toDottedClasspath(clazz.name());
    }

    @LuaWrapped
    public static String toSlashedClasspath(String className) {
        return className.replace('.', '/');
    }
}
