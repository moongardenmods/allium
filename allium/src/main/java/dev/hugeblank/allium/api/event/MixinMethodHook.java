package dev.hugeblank.allium.api.event;

import dev.hugeblank.allium.api.ScriptResource;
import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.lib.MixinLib;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import dev.hugeblank.allium.loader.type.annotation.OptionalArg;
import dev.hugeblank.allium.loader.type.coercion.TypeCoercions;
import dev.hugeblank.allium.loader.type.exception.InvalidArgumentException;
import dev.hugeblank.allium.util.asm.AsmUtil;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.objectweb.asm.Type;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.Dispatch;
import org.squiddev.cobalt.function.LuaFunction;

import java.util.ArrayList;
import java.util.List;

@LuaWrapped
public class MixinMethodHook {
    private final Script script;
    private final String id;
    private final List<Type> paramTypes;
    private final Type returnType;
    private final List<EClass<?>> paramClasses = new ArrayList<>();
    private EClass<?> returnClass;
    protected EventHandler handler;

    public MixinMethodHook(Script script, String id, List<Type> paramTypes, Type returnType) {
        this.script = script;
        this.id = id;
        this.paramTypes = paramTypes;
        this.returnType = returnType;
    }

    public static void create(Script script, String id, List<Type> paramTypes, Type returnType) {
        MixinLib.EVENT_MAP.put(id, new MixinMethodHook(script, id, paramTypes, returnType));
    }

    private static EClass<?> forName(String id, String name) {
        try {
            return EClass.fromJava(Class.forName(name));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("in hook "+id, e);
        }
    }

    @LuaWrapped
    public ScriptResource hook(LuaFunction func, @OptionalArg Boolean destroyOnUnload) {
        if (handler != null)
            throw new IllegalStateException("Mixin hook already registered for id '" + id + "' from " + script.getID());

        // This method should only be called once, and after preLaunch. Trusting that that's the case,
        // load the parameter and return type classes for this method.
        if (paramClasses.isEmpty()) {
            paramTypes.stream().map((type) -> forName(id, AsmUtil.getWrappedTypeName(type))).forEach(paramClasses::add);
        }
        if (!returnType.equals(Type.VOID_TYPE)) {
            returnClass = forName(id, AsmUtil.getWrappedTypeName(returnType));
        }

        if (destroyOnUnload == null) destroyOnUnload = true;
        handler = new EventHandler(func, script, destroyOnUnload);
        return handler;
    }

    public Object invoke(Object... objects) throws UnwindThrowable, LuaError, InvalidArgumentException {
        if (handler == null) {
            script.getLogger().warn("Mixin method '{}' missing hook", id);
            return null;
        }
        List<LuaValue> values = new ArrayList<>();
        int i = 0;
        for (EClass<?> argument : paramClasses) {
            values.add(TypeCoercions.toLuaValue(objects[i], argument));
            i++;
        }
        Varargs args = ValueFactory.varargsOf(values);
        return handler.handle(args);
    }

    protected class EventHandler implements ScriptResource {
        protected final LuaFunction func;
        protected final Script script;
        private final Script.ResourceRegistration registration;

        private EventHandler(LuaFunction func, Script script, boolean destroyOnUnload) {
            this.func = func;
            this.script = script;

            if (destroyOnUnload) {
                this.registration = script.registerResource(this);
            } else {
                this.registration = null;
            }
        }
        
        public Object handle(Varargs args) throws UnwindThrowable, LuaError, InvalidArgumentException {
            LuaState state = script.getState();
            LuaValue ret;
            try {
                synchronized (state) {
                        ret = Dispatch.invoke(state, func, args).first();
                }
                if (returnType.equals(Type.VOID_TYPE)) return null;
                return TypeCoercions.toJava(state, ret, returnClass);
            } catch (LuaError e) {
                script.getLogger().error("Error in mixin hook '{}'", id);
                throw e;
            }
        }

        @Override
        public void close() {
            handler = null;

            if (this.registration != null) {
                registration.close();
            }
        }
    }

    @Override
    public String toString() {
        return "MixinEventType{" + "id=" + id + '}';
    }
}
