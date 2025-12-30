package dev.hugeblank.allium.loader.type.property;

import me.basiqueevangelist.enhancedreflection.api.ModifierHolder;

import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.function.Predicate;

public record MemberFilter(boolean expectStatic, boolean expectPublic, boolean expectProtected,
                           boolean expectPrivate) implements Predicate<ModifierHolder> {

    public static final MemberFilter PUBLIC_STATIC_MEMBERS =
            new MemberFilter(true, true, false, false);

    public static final MemberFilter PUBLIC_MEMBERS =
            new MemberFilter(false, true, false, false);

    public static final MemberFilter CHILD_MEMBER_ACCESS =
            new MemberFilter(false, true, true, false);

    public static final MemberFilter STATIC_CHILD_MEMBER_ACCESS =
            new MemberFilter(true, true, true, false);

    public static final MemberFilter ALL_MEMBERS =
            new MemberFilter(false, true, true, true);

    @Override
    public boolean test(ModifierHolder holder) {
        return (expectStatic == holder.isStatic()) &&
                ((expectPublic == holder.isPublic()) ||
                (expectProtected == holder.isProtected()) ||
                (expectPrivate == holder.isPrivate()));
    }

    public MethodHandles.Lookup lookup(Class<?> clazz) throws IllegalAccessException {
        if (expectPrivate || expectProtected) {
            return MethodHandles.privateLookupIn(clazz, MethodHandles.lookup());
        } else {
            return MethodHandles.lookup();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MemberFilter(boolean aStatic, boolean aPublic, boolean aProtected, boolean aPrivate))) return false;
        return expectStatic == aStatic && expectPublic == aPublic && expectPrivate == aPrivate && expectProtected == aProtected;
    }

    @Override
    public int hashCode() {
        return Objects.hash(expectStatic, expectPublic, expectProtected, expectPrivate);
    }
}
