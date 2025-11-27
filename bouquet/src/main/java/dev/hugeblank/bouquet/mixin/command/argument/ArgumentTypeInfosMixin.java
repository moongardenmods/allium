package dev.hugeblank.bouquet.mixin.command.argument;

import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ArgumentTypeInfos.class)
public class ArgumentTypeInfosMixin {

//    @Inject(at = @At("HEAD"), method = "register")
//    private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<@NotNull A>> void register(Registry<@NotNull ArgumentTypeInfo<?, ?>> registry, String id, Class<? extends A> brigadierType, ArgumentTypeInfo<@NotNull A, @NotNull T> info, CallbackInfoReturnable<ArgumentTypeInfo<@NotNull A, @NotNull T>> cir) {
//        ArgumentTypeLib.addType(id, brigadierType);
//    }

}
