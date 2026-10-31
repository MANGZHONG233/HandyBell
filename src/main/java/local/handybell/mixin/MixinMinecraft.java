package local.handybell.mixin;

import local.handybell.core.CommandRunner;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Inject(method = "tick", at = @At("TAIL"))
    private void handybell$onTick(CallbackInfo ci) {
        CommandRunner.tick();
    }
}
