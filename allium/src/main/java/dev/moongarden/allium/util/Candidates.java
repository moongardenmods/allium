package dev.moongarden.allium.util;

import dev.moongarden.allium.loader.type.property.MemberFilter;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EField;
import me.basiqueevangelist.enhancedreflection.api.EMember;
import me.basiqueevangelist.enhancedreflection.api.EMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

public record Candidates(List<EMethod> methods, List<EField> fields, boolean isStatic) {

    public Stream<EMember> memberStream() {
        Stream.Builder<EMember> memberBuilder = Stream.builder();
        methods.forEach(memberBuilder);
        fields.forEach(memberBuilder);
        return memberBuilder.build();
    }

    public static Candidates derive(EClass<?> clazz, MemberFilter filter) {
        List<EMethod> methods = new ArrayList<>();
        List<EField> fields = new ArrayList<>();
        List<EClass<?>> interfaces = new ArrayList<>();
        Map<String, EClass<?>> nameMap = new HashMap<>();
        while (clazz != null) {
            methods.addAll(clazz.declaredMethods().stream().filter(testMember(nameMap, filter)).toList());
            interfaces.addAll(clazz.interfaces());
            fields.addAll(clazz.declaredFields().stream().filter(testMember(nameMap, filter)).toList());
            clazz = clazz.superclass();
            filter = filter.scopeUp();
        }
        for (EClass<?> iface : interfaces) {
            methods.addAll(iface.declaredMethods().stream().filter(testMember(nameMap, filter)).toList());
        }
        return new Candidates(methods, fields, filter.expectStatic());
    }

    private static Predicate<EMember> testMember(Map<String, EClass<?>> nameMap, MemberFilter filter) {
        return (m) -> {
            if (!filter.test(m)) return false;
            if (!nameMap.containsKey(m.name())) nameMap.put(m.name(), m.declaringClass());
            return nameMap.get(m.name()).equals(m.declaringClass());
        };
    }
}
