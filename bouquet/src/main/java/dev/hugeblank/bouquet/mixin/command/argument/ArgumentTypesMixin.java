package dev.hugeblank.bouquet.mixin.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import dev.hugeblank.bouquet.api.lib.commands.ArgumentTypeLib;
import net.minecraft.commands.arguments.coordinates.SwizzleArgument;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.execution.tasks.IsolatedCall;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SwizzleArgument.class)
public class ArgumentTypesMixin {

    @Inject(at = @At("HEAD"), method = "register(Lnet/minecraft/commands/execution/tasks/IsolatedCall;Ljava/lang/String;Ljava/lang/Class;Lnet/minecraft/commands/arguments/coordinates/RotationArgument;)Lnet/minecraft/commands/arguments/coordinates/RotationArgument;")
    private static <A extends ArgumentType<?>, T extends RotationArgument.ArgumentTypeProperties<A>> void register(IsolatedCall<RotationArgument<?, ?>> registry, String id, Class<? extends A> clazz, RotationArgument<A, T> serializer, CallbackInfoReturnable<RotationArgument<A, T>> cir) {
        ArgumentTypeLib.addType(id, clazz);
    }

}
