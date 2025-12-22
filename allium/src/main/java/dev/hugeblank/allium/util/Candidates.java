package dev.hugeblank.allium.util;

import me.basiqueevangelist.enhancedreflection.api.EField;
import me.basiqueevangelist.enhancedreflection.api.EMember;
import me.basiqueevangelist.enhancedreflection.api.EMethod;

import java.util.List;
import java.util.stream.Stream;

public record Candidates(List<EMethod> methods, List<EField> fields) {

    public Stream<EMember> memberStream() {
        Stream.Builder<EMember> memberBuilder = Stream.builder();
        methods.forEach(memberBuilder);
        fields.forEach(memberBuilder);
        return memberBuilder.build();
    }
}
