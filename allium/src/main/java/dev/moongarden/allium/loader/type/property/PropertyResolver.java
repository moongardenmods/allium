package dev.moongarden.allium.loader.type.property;

import dev.moongarden.allium.Allium;
import dev.moongarden.allium.util.AnnotationUtils;
import dev.moongarden.allium.util.Candidates;
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

    public static <T> PropertyData<? super T> resolveProperty(EClass<T> clazz, String name, Candidates candidates) {
        List<EMethod> methods = candidates.methods();

        if (!methods.isEmpty()) {
            List<EMethod> foundMethods = new ArrayList<>();

            collectMethods(methods, name, foundMethods::add);

            if (!foundMethods.isEmpty())
                return new MethodData<>(clazz, foundMethods, name, candidates.isStatic());

            EMethod getter = findMethod(methods, "get" + StringUtils.capitalize(name),
                    method -> AnnotationUtils.countLuaArguments(method) == 0 && method.isStatic());

            if (getter != null) {
                EMethod setter = findMethod(methods, "set" + StringUtils.capitalize(name),
                        method -> AnnotationUtils.countLuaArguments(method) == 1 && method.isStatic());

                return new PropertyMethodData<>(getter, setter);
            }
        }

        List<EField> fields = candidates.fields();

        if (!fields.isEmpty()) {
            EField field = findField(fields, name);

            if (field != null) {
                try {
                    return new FieldData<>(field);
                } catch (IllegalAccessException e) {
                    Allium.LOGGER.warn("Attempt to access '{}' resulted in: ", field.name(), e);
                }
            }
        }

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

            if ((methodName.equals(name) || name.equals("m_" + methodName) || methodName.equals("allium$" + name)) && !methodName.startsWith("allium_private$")) {
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

            if ((methodName.equals(name) || name.equals("m_" + methodName) || methodName.equals("allium$" + name)) && !methodName.startsWith("allium_private$")) {
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

            String fieldName = field.name();

            if ((fieldName.equals(name) || name.equals("f_" + fieldName) || fieldName.equals("allium$")) && !fieldName.startsWith("allium_private$")) {
                return field;
            }
        }

        return null;
    }
}
