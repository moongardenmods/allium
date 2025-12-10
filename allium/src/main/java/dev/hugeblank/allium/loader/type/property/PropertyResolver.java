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

    public static <T> PropertyData<? super T> resolveProperty(EClass<T> clazz, String name, MemberFilter filter) {
        List<EMethod> methods = new ArrayList<>(clazz.methods());
        methods.addAll(clazz.declaredMethods());
        List<EField> fields = new ArrayList<>(clazz.fields());
        fields.addAll(clazz.declaredFields());
        return resolvePropertyFrom(
                clazz,
                methods.stream().distinct().filter(filter).toList(),
                fields.stream().distinct().filter(filter).toList(),
                name,
                filter
        );
    }

    public static <T> PropertyData<? super T> resolvePropertyFrom(EClass<T> clazz, List<EMethod> methods, Collection<EField> fields, String name, MemberFilter filter) {
        List<EMethod> foundMethods = new ArrayList<>();

        collectMethods(methods, name, foundMethods::add);

        if (!foundMethods.isEmpty())
            return new MethodData<>(clazz, foundMethods, name, filter);

        EMethod getter = findMethod(methods, "get" + StringUtils.capitalize(name),
                method -> AnnotationUtils.countLuaArguments(method) == 0 && method.isStatic());

        if (getter != null) {
            EMethod setter = findMethod(methods, "set" + StringUtils.capitalize(name),
                    method -> AnnotationUtils.countLuaArguments(method) == 1 && method.isStatic());

            return new PropertyMethodData<>(getter, setter);
        }

        EField field = findField(fields, name);

        if (field != null)
            return new FieldData<>(field);

        return EmptyData.INSTANCE;
    }

    public static void collectMethods(Collection<EMethod> methods, String name, Consumer<EMethod> consumer) {
        for (EMethod method : methods) {
            if (AnnotationUtils.isHiddenFromLua(method)) continue;

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

    public static EField findField(Collection<EField> fields, String name) {
        for (var field : fields) {
            if (AnnotationUtils.isHiddenFromLua(field)) continue;

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
