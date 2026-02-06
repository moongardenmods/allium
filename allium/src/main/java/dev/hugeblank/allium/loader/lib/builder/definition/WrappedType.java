package dev.hugeblank.allium.loader.lib.builder.definition;

import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EParameter;

public record WrappedType(EClass<?> raw, EClass<?> real) {
    public static WrappedType fromParameter(EParameter param) {
        return new WrappedType(param.rawParameterType(), param.parameterType().lowerBound());
    }
}
