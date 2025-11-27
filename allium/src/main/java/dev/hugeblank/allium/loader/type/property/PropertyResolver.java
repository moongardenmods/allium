package dev.hugeblank.allium.loader.type.property;

import dev.hugeblank.allium.util.AnnotationUtils;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EField;
import me.basiqueevangelist.enhancedreflection.api.EMethod;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class PropertyResolver {

    public static <T> PropertyData<? super T> resolveProperty(EClass<T> clazz, String name, boolean isStatic) {
        List<EMethod> foundMethods = new ArrayList<>();

        collectMethods(clazz.methods(), name, isStatic, foundMethods::add);

        if (!foundMethods.isEmpty())
            return new MethodData<>(clazz, foundMethods, name, isStatic);

        EMethod getter = findMethod(clazz.methods(), "get" + StringUtils.capitalize(name),
            method -> AnnotationUtils.countLuaArguments(method) == 0 && (!isStatic || method.isStatic()));

        if (getter != null) {
            EMethod setter = findMethod(clazz.methods(), "set" + StringUtils.capitalize(name),
                method -> AnnotationUtils.countLuaArguments(method) == 1 && (!isStatic || method.isStatic()));

            return new PropertyMethodData<>(getter, setter);
        }

        EField field = findField(clazz.fields(), name, isStatic);

        if (field != null)
            return new FieldData<>(field);

        return EmptyData.INSTANCE;
    }

    public static void collectMethods(Collection<EMethod> methods, String name, boolean staticOnly, Consumer<EMethod> consumer) {
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

            if ((methodName.equals(name) || methodName.equals("allium$" + name) || name.equals("m_" + methodName)) && !methodName.startsWith("allium_private$")) {
                consumer.accept(method);
            }
        }
    }

    public static EMethod findMethod(List<EMethod> methods, String name, Predicate<EMethod> filter) {
        for (EMethod method : methods) {
            if (AnnotationUtils.isHiddenFromLua(method)) continue;
            if (!filter.test(method)) continue;

            String[] altNames = AnnotationUtils.findNames(method);
            if (altNames != null) {
                for (String altName : altNames) {
                    if (altName.equals(name)) return method;
                }

                continue;
            }

            var methodName = method.name();

            if ((methodName.equals(name) || methodName.equals("allium$" + name) || name.equals("m_" + methodName)) && !methodName.startsWith("allium_private$")) {
                return method;
            }
        }

        return null;
    }

    public static EField findField(Collection<EField> fields, String name, boolean staticOnly) {
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

            if (field.name().equals(name)) {
                return field;
            }
        }

        return null;
    }
}
