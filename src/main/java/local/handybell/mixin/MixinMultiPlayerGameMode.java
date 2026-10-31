package local.handybell.mixin;

import local.handybell.core.BellHandler;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void handybell$beforeUseItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult hit,
                                           CallbackInfoReturnable<InteractionResult> cir) {
        if (hand != InteractionHand.MAIN_HAND) {
            return;
        }
        InteractionResult result = BellHandler.onUseItemOn(player, hand, hit);
        if (result != null) {
            cir.setReturnValue(result);
        }
    }

    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void handybell$afterUseItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult hit,
                                          CallbackInfoReturnable<InteractionResult> cir) {
        if (hand != InteractionHand.MAIN_HAND) {
            return;
        }
        InteractionResult result = cir.getReturnValue();
        if (result != null && result.consumesAction()) {
            BellHandler.afterUseItemOn(player, hand, hit);
        }
    }
}
