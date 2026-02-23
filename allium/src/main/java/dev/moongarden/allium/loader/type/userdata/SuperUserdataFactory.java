package dev.moongarden.allium.loader.type.userdata;

import dev.moongarden.allium.loader.type.property.MemberFilter;
import me.basiqueevangelist.enhancedreflection.api.EClass;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SuperUserdataFactory<T> extends AbstractUserdataFactory<T, SuperUserdata<T>> {
    private static final ConcurrentMap<EClass<?>, SuperUserdataFactory<?>> FACTORIES = new ConcurrentHashMap<>();

    SuperUserdataFactory(EClass<T> clazz) {
        super(clazz, clazz.superclass(), MemberFilter.PARENT_MEMBERS);
    }

    @Override
    public SuperUserdata<T> create(Object instance) {
        //noinspection unchecked
        return new SuperUserdata<>((T) instance, metatable, clazz);
    }

    @Override
    public SuperUserdata<T> createBound(Object instance) {
        super.initBound();

        //noinspection unchecked
        return new SuperUserdata<>((T) instance, boundMetatable, clazz);
    }

    @SuppressWarnings("unchecked")
    public static <T> SuperUserdataFactory<T> from(EClass<T> clazz) {
        return (SuperUserdataFactory<T>) FACTORIES.computeIfAbsent(clazz, SuperUserdataFactory::new);
    }
}
