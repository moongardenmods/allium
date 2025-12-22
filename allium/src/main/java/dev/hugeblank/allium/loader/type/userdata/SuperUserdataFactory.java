package dev.hugeblank.allium.loader.type.userdata;

import dev.hugeblank.allium.loader.type.property.MemberFilter;
import me.basiqueevangelist.enhancedreflection.api.EClass;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SuperUserdataFactory<T> extends AbstractUserdataFactory<T, SuperUserdata<T>> {
    private static final ConcurrentMap<EClass<?>, SuperUserdataFactory<?>> FACTORIES = new ConcurrentHashMap<>();

    // TODO: Provide a way to access protected static variables
    // Indexing a name that can't exist on the java side. Maybe there's a better way to do this?
//        if (name.equals("static")) {
//            return StaticBinder.bindClass(superClass, MemberFilter.STATIC_CHILD_MEMBER_ACCESS);
//        }

    SuperUserdataFactory(EClass<T> clazz) {
        super(clazz, clazz.superclass(), MemberFilter.CHILD_MEMBER_ACCESS);
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
