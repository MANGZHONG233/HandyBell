package local.handybell.mixin;

import local.handybell.core.HandyBellCommand;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在聊天指令发出之前截下 {@code /handybell ...}。
 * <p>
 * 这些指令是纯客户端设置，不需要服务端参与；拦下来既能保证在别人的服务器上也能用，
 * 也不会在服务端日志里留下"未知指令"的报错。
 */
@Mixin(ClientPacketListener.class)
public class MixinChatCommand {

    @Inject(method = "sendCommand", at = @At("HEAD"), cancellable = true, require = 0)
    private void handybell$interceptCommand(String command, CallbackInfo ci) {
        if (HandyBellCommand.handle(command)) {
            ci.cancel();
        }
    }
}
