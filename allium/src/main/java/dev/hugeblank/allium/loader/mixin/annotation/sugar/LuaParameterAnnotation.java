package dev.hugeblank.allium.loader.mixin.annotation.sugar;

import dev.hugeblank.allium.loader.mixin.annotation.LuaAnnotationParser;
import dev.hugeblank.allium.loader.type.exception.InvalidArgumentException;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.objectweb.asm.Type;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;

import java.util.HashMap;
import java.util.Map;

public abstract class LuaParameterAnnotation implements LuaSugar {
    private static final String LOCALREF_PACKAGE_PREFIX = "Lcom/llamalad7/mixinextras/sugar/ref/";
    private static final Map<Type, String> TYPE_TO_REF;
    protected final String type;
    protected final LuaAnnotationParser parser;

    public LuaParameterAnnotation(LuaState state, String type, LuaTable annotationTable, Class<?> annotation) throws InvalidArgumentException, LuaError {
        this.type = type;
        this.parser = new LuaAnnotationParser(state, annotationTable, EClass.fromJava(annotation));
    }

    public String type() {
        return type;
    }

    public LuaAnnotationParser luaAnnotation() {
        return parser;
    }


    protected String toRefType() {
        if (type.startsWith(LOCALREF_PACKAGE_PREFIX)) {
            return type;
        }
        Type curType = Type.getType(type);
        return  LOCALREF_PACKAGE_PREFIX + TYPE_TO_REF.getOrDefault(curType, "LocalRef;");
    }

    static {
        TYPE_TO_REF = new HashMap<>();
        TYPE_TO_REF.put(Type.BOOLEAN_TYPE, "LocalBooleanRef;");
        TYPE_TO_REF.put(Type.BYTE_TYPE, "LocalByteRef;");
        TYPE_TO_REF.put(Type.CHAR_TYPE, "LocalCharRef;");
        TYPE_TO_REF.put(Type.DOUBLE_TYPE, "LocalDoubleRef;");
        TYPE_TO_REF.put(Type.FLOAT_TYPE, "LocalFloatRef;");
        TYPE_TO_REF.put(Type.INT_TYPE, "LocalIntRef;");
        TYPE_TO_REF.put(Type.LONG_TYPE, "LocalLongRef;");
        TYPE_TO_REF.put(Type.SHORT_TYPE, "LocalShortRef;");
    }
}
