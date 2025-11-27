package dev.hugeblank.allium.api.event;

import dev.hugeblank.allium.api.ScriptResource;
import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import dev.hugeblank.allium.loader.type.annotation.OptionalArg;
import dev.hugeblank.allium.loader.type.coercion.TypeCoercions;
import dev.hugeblank.allium.loader.type.exception.InvalidArgumentException;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.Dispatch;
import org.squiddev.cobalt.function.LuaFunction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@LuaWrapped
public class MixinMethodHook {
    public static final Map<String, MixinMethodHook> EVENT_MAP = new HashMap<>();
    private final Script script;
    private final String id;
    private final List<String> definitions;
    private final List<EClass<?>> arguments = new ArrayList<>();
    protected EventHandler handler;

    public MixinMethodHook(Script script, String id, List<String> definitions) {
        this.id = id;
        this.definitions = definitions;
        this.script = script;
        EVENT_MAP.put(id, this);
    }

    private static EClass<?> forName(String id, String name) {
        try {
            // Surely this won't cause any issues in the future!
            return EClass.fromJava(Class.forName(name));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("in hook "+id, e);
        }
    }

    @LuaWrapped
    public ScriptResource hook(LuaFunction func, @OptionalArg Boolean destroyOnUnload) {
        if (handler != null)
            throw new IllegalStateException("Mixin hook already registered for id '" + id + "' from " + script.getID());

        if (destroyOnUnload == null) destroyOnUnload = true;
        handler = new EventHandler(func, script, destroyOnUnload);
        return handler;
    }

    public Object invoke(Object... objects) throws UnwindThrowable, LuaError, InvalidArgumentException {
        if (handler == null) {
            script.getLogger().warn("Mixin method '{}' missing hook", id);
        }
        if (arguments.isEmpty()) {
            definitions.forEach((def) -> arguments.add(forName(id, def)));
        }
        List<LuaValue> values = new ArrayList<>();
        int i = 0;
        for (EClass<?> argument : arguments) {
            values.add(TypeCoercions.toLuaValue(objects[i], argument));
            i++;
        }
        Varargs args = ValueFactory.varargsOf(values);
        return handler == null ? null : handler.handle(args);
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
            LuaState state = script.getExecutor().getState();
            LuaValue ret;
            try {
                synchronized (state) {
                        ret = Dispatch.invoke(state, func, args).first();
                }
                return TypeCoercions.toJava(state, ret, Object.class);
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
