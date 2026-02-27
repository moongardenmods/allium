package dev.moongarden.allium.util.combine;

import dev.moongarden.allium.api.LuaStateArg;
import dev.moongarden.allium.api.LuaWrapped;
import dev.moongarden.allium.api.OptionalArg;
import dev.moongarden.combine.extension.api.*;
import dev.moongarden.combine.extension.impl.AnnotationParserExtensionImpl;
import dev.moongarden.combine.extension.impl.ClassParserExtensionImpl;
import dev.moongarden.combine.extension.impl.FieldParserExtensionImpl;
import dev.moongarden.combine.extension.impl.MethodParserExtensionImpl;
import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AlliumCombineExtension implements CombineExtension {
    private static final String WRAPPED = Type.getDescriptor(LuaWrapped.class);
    private static final String STATE_ARG = Type.getDescriptor(LuaStateArg.class);
    private static final String OPTIONAL_ARG = Type.getDescriptor(OptionalArg.class);

    @Override
    public ClassParserExtension createClassParser() {
        return new ClassParserExtensionImpl() {
            private boolean isWrapped = false;

            @Override
            public AnnotationParserExtension visitAnnotation(String descriptor, boolean visible) {
                if (descriptor.equals(WRAPPED)) isWrapped = true;
                return null;
            }

            @Override
            public MethodParserExtension visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                return new MethodParserExtensionImpl() {
                    private String methodName = name;
                    private boolean hasWrapped = false;
                    private final Map<Integer, Boolean> shouldWriteParams = new HashMap<>();
                    private final Map<Integer, Boolean> optionalParams = new HashMap<>();

                    @Override
                    public AnnotationParserExtension visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
                        shouldWriteParams.put(parameter, !descriptor.equals(STATE_ARG));
                        optionalParams.put(parameter, descriptor.equals(OPTIONAL_ARG));
                        return super.visitParameterAnnotation(parameter, descriptor, visible);
                    }

                    @Override
                    public boolean shouldWriteParameter(int parameter) {
                        return shouldWriteParams.containsKey(parameter) ?
                            shouldWriteParams.get(parameter) :
                            super.shouldWriteParameter(parameter);
                    }

                    @Override
                    public String modifyParameterType(int parameter, String type) {
                        if (type.equals("java.lang.String")) type = "string";
                        return optionalParams.containsKey(parameter) && optionalParams.get(parameter) ?
                            type + "?" : type;
                    }

                    @Override
                    public AnnotationParserExtension visitAnnotation(String descriptor, boolean visible) {
                        if (descriptor.equals(WRAPPED)) {
                            hasWrapped = true;
                            return new AnnotationParserExtensionImpl() {
                                @Override
                                public AnnotationParserExtension visitArray(String key) {
                                    // "name" method on LuaWrapped is a String array.
                                    return new AnnotationParserExtensionImpl() {
                                        @Override
                                        public void visit(String index, Object value) {
                                            if (key.equals("name")) methodName = value.toString();
                                        }
                                    };
                                }

                            };
                        }
                        return null;
                    }

                    @Override
                    public String modifyMethodName(String name) {
                        return methodName;
                    }

                    @Override
                    public boolean shouldWriteMethod() {
                        if (isWrapped) {
                            return hasWrapped;
                        }
                        return true;
                    }
                };
            }

            @Override
            public FieldParserExtension visitField(int access, String name, String descriptor, String signature, Object value) {
                return new FieldParserExtensionImpl() {
                    private String fieldName = name;
                    private boolean hasWrapped = false;
                    @Override
                    public AnnotationParserExtension visitAnnotation(String descriptor, boolean visible) {
                        if (descriptor.equals(WRAPPED)) {
                            hasWrapped = true;
                            return new AnnotationParserExtensionImpl() {
                                @Override
                                public AnnotationParserExtension visitArray(String key) {
                                    return new AnnotationParserExtensionImpl() {
                                        @Override
                                        public void visit(String index, Object value) {
                                            if (key.equals("name")) fieldName = value.toString();
                                        }
                                    };
                                }

                            };
                        }
                        return null;
                    }

                    @Override
                    public String modifyFieldName(String name) {
                        return fieldName;
                    }

                    @Override
                    public boolean shouldWriteField() {
                        if (isWrapped) {
                            return hasWrapped;
                        }
                        return true;
                    }
                };
            }
        };
    }
}
