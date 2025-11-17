package dev.hugeblank.bouquet.mixin.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import dev.hugeblank.bouquet.api.lib.commands.ArgumentTypeLib;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.Registry;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArgumentTypeInfos.class)
public class ArgumentTypeInfosMixin {

    @Inject(at = @At("HEAD"), method = "register")
    private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<@NotNull A>> void register(Registry<@NotNull ArgumentTypeInfo<?, ?>> registry, String id, Class<? extends A> brigadierType, ArgumentTypeInfo<@NotNull A, @NotNull T> info, CallbackInfoReturnable<ArgumentTypeInfo<@NotNull A, @NotNull T>> cir) {
        ArgumentTypeLib.addType(id, brigadierType);
    }

}
