// Eldritch horrors, sponsored by allium!
// This class converts all public methods from any class from Java -> Lua.
// Completely unrestrained, interprets everything. I'm sorry.
// If someone wants to SCP this, please by all means do so.
package dev.moongarden.allium.loader.type.userdata;

import me.basiqueevangelist.enhancedreflection.api.EClass;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InstanceUserdataFactory<T> extends AbstractUserdataFactory<T, InstanceUserdata<T>> {
    private static final ConcurrentMap<EClass<?>, InstanceUserdataFactory<?>> FACTORIES = new ConcurrentHashMap<>();

    protected InstanceUserdataFactory(EClass<T> clazz) {
        super(clazz);
    }

    @SuppressWarnings("unchecked")
    public static <T> InstanceUserdataFactory<T> from(EClass<T> clazz) {
        return (InstanceUserdataFactory<T>) FACTORIES.computeIfAbsent(clazz, InstanceUserdataFactory::new);
    }

    @Override
    public InstanceUserdata<T> create(Object instance) {
        //noinspection unchecked
        return new InstanceUserdata<>((T) instance, metatable, clazz);
    }

    @Override
    public InstanceUserdata<T> createBound(Object instance) {
        super.initBound();

        //noinspection unchecked
        return new InstanceUserdata<>((T) instance, boundMetatable, clazz);
    }

}
