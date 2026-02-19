package dev.hugeblank.allium.loader.type.userdata;

import dev.hugeblank.allium.loader.type.property.MemberFilter;
import dev.hugeblank.allium.util.Candidates;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EField;
import me.basiqueevangelist.enhancedreflection.api.EMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PrivateUserdataFactory<T> extends AbstractUserdataFactory<T, PrivateUserdata<T>>{
    private static final ConcurrentMap<EClass<?>, PrivateUserdataFactory<?>> FACTORIES = new ConcurrentHashMap<>();

    PrivateUserdataFactory(EClass<T> clazz) {
        super(clazz, MemberFilter.ALL_MEMBERS);
    }

//    @Override
//    protected Candidates deriveCandidates(EClass<?> clazz, MemberFilter filter) {
//        List<EMethod> methods = new ArrayList<>(clazz.declaredMethods().stream().filter(filter).toList());
//        List<EField> fields = clazz.declaredFields().stream().filter(filter).toList();
//        clazz.interfaces().forEach((iface) ->
//                methods.addAll(iface.declaredMethods().stream().filter(filter).toList())
//        );
//        return new Candidates(methods, fields);
//    }

    @SuppressWarnings("unchecked")
    public static <T> PrivateUserdataFactory<T> from(EClass<T> clazz) {
        return (PrivateUserdataFactory<T>) FACTORIES.computeIfAbsent(clazz, PrivateUserdataFactory::new);
    }

    @Override
    public PrivateUserdata<T> create(Object instance) {
        //noinspection unchecked
        return new PrivateUserdata<>((T) instance, metatable, clazz);
    }

    @Override
    public PrivateUserdata<T> createBound(Object instance) {
        super.initBound();

        //noinspection unchecked
        return new PrivateUserdata<>((T) instance, boundMetatable, clazz);
    }
}
