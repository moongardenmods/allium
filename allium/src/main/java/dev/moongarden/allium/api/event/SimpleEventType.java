package dev.moongarden.allium.api.event;

import me.basiqueevangelist.enhancedreflection.api.EClass;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

/// Helper class for creating events.
/// Building off of `EventType`, this class adds a proxy class `invoker` that is derived from a given
/// factory interface `T`, that wraps around registered lua handlers.
///
/// This class and `EventType` are not used internally by Allium and are provided solely for convenience. Use them!
public class SimpleEventType<T> extends EventType<T> {
    private final EClass<T> eventType;
    private final T invoker;

    @SuppressWarnings("unchecked")
    public SimpleEventType(T... typeGetter) {
        this((EClass<T>) EClass.fromJava(typeGetter.getClass().componentType()));
    }

    @SuppressWarnings("unchecked")
    public SimpleEventType(EClass<T> eventType) {
        this.eventType = eventType;

        this.invoker = (T) Proxy.newProxyInstance(SimpleEventType.class.getClassLoader(), new Class[]{eventType.raw()}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "equals" -> {
                    return proxy == args[0];
                }
                case "hashCode" -> {
                    return System.identityHashCode(proxy);
                }
                case "toString" -> {
                    return "<invoker for " + eventType + ">";
                }
            }
            
            if (method.isDefault())
                return InvocationHandler.invokeDefault(proxy, method, args);

            for (EventHandler handler : handlers) {
                try {
                    method.invoke(handler.handler, args);
                } catch (InvocationTargetException e) {
                    handler.script.getLogger().error("Error while handling event {}", eventType.name(), e.getTargetException());
                }
            }

            return null;
        });
    }

    public final T invoker() {
        return invoker;
    }

    @Override
    public String toString() {
        return "SimpleEventType{" +
            "type=" + eventType.name() +
            '}';
    }
}
