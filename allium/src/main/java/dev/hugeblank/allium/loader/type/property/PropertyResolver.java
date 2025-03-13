package dev.hugeblank.allium.loader.type.property;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.loader.ScriptRegistry;
import dev.hugeblank.allium.util.AnnotationUtils;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EField;
import me.basiqueevangelist.enhancedreflection.api.EMethod;
import dev.hugeblank.allium.mappings.Mappings;
import org.apache.commons.lang3.StringUtils;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class PropertyResolver {
    private PropertyResolver() {

    }

    public static <T> PropertyData<? super T> resolveProperty(LuaState state, EClass<T> clazz, String name, boolean isStatic) throws LuaError {
        List<EMethod> foundMethods = new ArrayList<>();

        collectMethods(state, clazz, clazz.methods(), name, isStatic, foundMethods::add);

        if (!foundMethods.isEmpty())
            return new MethodData<>(clazz, foundMethods, name, isStatic);

        EMethod getter = findMethod(state, clazz, clazz.methods(), "get" + StringUtils.capitalize(name),
            method -> AnnotationUtils.countLuaArguments(method) == 0 && (!isStatic || method.isStatic()));

        if (getter != null) {
            EMethod setter = findMethod(state, clazz, clazz.methods(), "set" + StringUtils.capitalize(name),
                method -> AnnotationUtils.countLuaArguments(method) == 1 && (!isStatic || method.isStatic()));

            return new PropertyMethodData<>(getter, setter);
        }

        EField field = findField(state, clazz, clazz.fields(), name, isStatic);

        if (field != null)
            return new FieldData<>(field);

        return EmptyData.INSTANCE;
    }

    public static void collectMethods(LuaState state, EClass<?> sourceClass, Collection<EMethod> methods, String name, boolean staticOnly, Consumer<EMethod> consumer) throws LuaError {
        for (EMethod method : methods) {
            if (AnnotationUtils.isHiddenFromLua(method)) continue;
            if (staticOnly && !method.isStatic()) continue;

            String[] altNames = AnnotationUtils.findNames(method);
            if (altNames != null) {
                for (String altName : altNames) {
                    if (altName.equals(name)) {
                        consumer.accept(method);
                    }
                }

                continue;
            }

            var methodName = method.name();

            if (methodName.equals(name) || methodName.equals("allium$" + name) || name.equals("m_" + methodName)) {
                consumer.accept(method);
            }

            if (methodName.startsWith("allium_private$")) {
                continue;
            }


            if (!Allium.DEVELOPMENT) {
                Mappings mappings = ScriptRegistry.find(state).getMappings();

                var mappedName = mappings.getMapped(Mappings.asMethod(sourceClass, method)).split("#")[1];
                if (mappedName.equals(name) || mappedName.equals("m_" + methodName)) {
                    consumer.accept(method);
                }

                for (var clazz : sourceClass.allInterfaces()) {
                    mappedName = mappings.getMapped(Mappings.asMethod(clazz, method)).split("#")[1];
                    if (mappedName.equals(name) || mappedName.equals("m_" + methodName)) {
                        consumer.accept(method);
                    }
                }

                for (var clazz : sourceClass.allSuperclasses()) {
                    mappedName = mappings.getMapped(Mappings.asMethod(clazz, method)).split("#")[1];
                    if (mappedName.equals(name) || mappedName.equals("m_" + methodName)) {
                        consumer.accept(method);
                    }

                    for (var iface : clazz.allInterfaces()) {
                        mappedName = mappings.getMapped(Mappings.asMethod(iface, method)).split("#")[1];
                        if (mappedName.equals(name) || mappedName.equals("m_" + methodName)) {
                            consumer.accept(method);
                        }
                    }
                }
            }
        }
    }

    public static EMethod findMethod(LuaState state, EClass<?> sourceClass, List<EMethod> methods, String name, Predicate<EMethod> filter) throws LuaError {
        for (EMethod method : methods) {
            if (AnnotationUtils.isHiddenFromLua(method)) continue;
            if (!filter.test(method)) continue;

            String[] altNames = AnnotationUtils.findNames(method);
            if (altNames != null) {
                for (String altName : altNames) {
                    if (altName.equals(name)) {
                        return method;
                    }
                }

                continue;
            }

            var methodName = method.name();

            if (methodName.equals(name) || methodName.equals("allium$" + name) || name.equals("m_" + methodName)) {
                return method;
            }

            if (methodName.startsWith("allium_private$")) {
                continue;
            }

            if (!Allium.DEVELOPMENT) {
                Mappings mappings = ScriptRegistry.find(state).getMappings();

                var mappedName = mappings.getMapped(Mappings.asMethod(sourceClass, method)).split("#")[1];
                if (mappedName.equals(name) || mappedName.equals("m_" + methodName)) {
                    return method;
                }

                for (var clazz : sourceClass.allSuperclasses()) {
                    mappedName = mappings.getMapped(Mappings.asMethod(clazz, method)).split("#")[1];
                    if (mappedName.equals(name) || mappedName.equals("m_" + methodName)) {
                        return method;
                    }
                }

                for (var clazz : sourceClass.allInterfaces()) {
                    mappedName = mappings.getMapped(Mappings.asMethod(clazz, method)).split("#")[1];
                    if (mappedName.equals(name) || mappedName.equals("m_" + methodName)) {
                        return method;
                    }
                }
            }
        }

        return null;
    }

    public static EField findField(LuaState state, EClass<?> sourceClass, Collection<EField> fields, String name, boolean staticOnly) throws LuaError {
        for (var field : fields) {
            if (AnnotationUtils.isHiddenFromLua(field)) continue;
            if (staticOnly && !field.isStatic()) continue;

            String[] altNames = AnnotationUtils.findNames(field);
            if (altNames != null) {
                for (String altName : altNames) {
                    if (altName.equals(name)) {
                        return field;
                    }
                }

                continue;
            }

            if (Allium.DEVELOPMENT) {
                if (field.name().equals(name)) {
                    return field;
                }
            } else {
                Mappings mappings = ScriptRegistry.find(state).getMappings();

                if (mappings.getMapped(Mappings.asMethod(sourceClass, field)).split("#")[1].equals(name)) {
                    return field;
                }

                for (var clazz : sourceClass.allSuperclasses()) {
                    if (mappings.getMapped(Mappings.asMethod(clazz, field)).split("#")[1].equals(name)) {
                        return field;
                    }
                }
            }
        }

        return null;
    }
}
