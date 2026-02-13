package dev.hugeblank.allium.util.asm;

import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.ScriptExecutor;
import dev.hugeblank.allium.loader.ScriptRegistry;
import dev.hugeblank.allium.loader.lib.MixinLib;
import dev.hugeblank.allium.loader.lib.builder.ClassBuilder;
import dev.hugeblank.allium.loader.type.StaticBinder;
import dev.hugeblank.allium.loader.type.coercion.TypeCoercions;
import dev.hugeblank.allium.loader.type.userdata.*;
import dev.hugeblank.allium.util.Pair;
import dev.hugeblank.allium.util.Registry;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.objectweb.asm.Type;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.ValueFactory;
import org.squiddev.cobalt.Varargs;
import org.squiddev.cobalt.function.Dispatch;

import java.util.List;
import java.util.Map;

public class Owners {
    // Java
    public static final String OBJECT = Type.getInternalName(Object.class);
    public static final String LIST = Type.getInternalName(List.class);
    public static final String MAP = Type.getInternalName(Map.class);
    // Enhanced Reflection
    public static final String ECLASS = Type.getInternalName(EClass.class);
    // Allium
    public static final String PAIR = Type.getInternalName(Pair.class);
    public static final String SCRIPT = Type.getInternalName(Script.class);
    public static final String SCRIPT_EXECUTOR = Type.getInternalName(ScriptExecutor.class);
    public static final String MIXIN_LIB = Type.getInternalName(MixinLib.class);
    public static final String SCRIPT_REGISTRY = Type.getInternalName(ScriptRegistry.class);
    public static final String REGISTRY = Type.getInternalName(Registry.class);
    public static final String INSTANCE_USERDATA_FACTORY = Type.getInternalName(InstanceUserdataFactory.class);
    public static final String PRIVATE_USERDATA_FACTORY = Type.getInternalName(PrivateUserdataFactory.class);
    public static final String PRIVATE_USERDATA = Type.getInternalName(PrivateUserdata.class);
    public static final String SUPER_USERDATA_FACTORY = Type.getInternalName(SuperUserdataFactory.class);
    public static final String SUPER_USERDATA = Type.getInternalName(SuperUserdata.class);
    public static final String STATIC_BINDER = Type.getInternalName(StaticBinder.class);
    public static final String TYPE_COERCIONS = Type.getInternalName(TypeCoercions.class);
    public static final String CLASS_BUILDER = Type.getInternalName(ClassBuilder.class);
    public static final String FIELD_HOLDER = Type.getInternalName(ClassBuilder.FieldHolder.class);
    // Cobalt
    public static final String LUA_VALUE = Type.getInternalName(LuaValue.class);
    public static final String VARARGS = Type.getInternalName(Varargs.class);
    public static final String VALUE_FACTORY = Type.getInternalName(ValueFactory.class);
    public static final String DISPATCH = Type.getInternalName(Dispatch.class);
}
