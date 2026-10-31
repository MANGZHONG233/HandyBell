package local.handybell.mixin;

import local.handybell.core.BellHandler;
import local.handybell.core.CommandRunner;
import local.handybell.store.BellSpot;
import local.handybell.store.HandyBellStore;
import local.handybell.store.SlotConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * 钟实体相关的两处拦截：
 * <ol>
 *   <li>给事钟敲响时不冒粒子；</li>
 *   <li>钟被"非玩家右键"的方式敲响时，执行本客户端上配置的那条命令。</li>
 * </ol>
 * 注意：不要动 updateEntities()——它同时负责初始化 nearbyEntities 字段，
 * 跳过后 tick() 会拿去遍历，直接 NPE 闪退。只在粒子播放入口拦截，安全得多。
 */
@Mixin(BellBlockEntity.class)
public class MixinBellBlockEntity {

    @Inject(method = "showBellParticles", at = @At("HEAD"), cancellable = true, require = 0)
    private static void handybell$noParticles(Level level, BlockPos pos, List<LivingEntity> entities, CallbackInfo ci) {
        if (level == null || pos == null) {
            return;
        }
        if (HandyBellStore.get().spot(level, pos) != null) {
            ci.cancel();
        }
    }

    /**
     * 钟响时执行命令——<b>每个客户端各自响应自己的配置</b>。
     * <p>
     * 为什么挂在客户端 {@code triggerEvent} 而不是服务端 {@code onHit}：
     * 服务端的 onHit 只在房主的进程里跑，局域网联机时只有房主那份配置会生效，
     * 其他人配的命令永远不会触发。改挂客户端后，谁装了模组、谁配了命令，就各自执行各自的，
     * 并且命令是以各自的身份发出的（权限也各归各）。顺带多人服也能用，不再局限单人。
     * <p>
     * 判定规则仍然完全交给原版：红石怎么充能、投掷物命中、响几次，全由
     * {@code BellBlock.neighborChanged} / {@code onProjectileHit} 决定。
     * 我们只挂"已经确定要响"的那一刻。
     * <p>
     * 玩家自己右键不会走到这里：那条路径在 {@code BellHandler.ringLocally} 里
     * 也会产生一次 blockEvent，靠 {@code BellHandler.LOCAL_RING} 标记跳过，
     * 否则同一条命令会执行两遍。
     */
    @Inject(method = "triggerEvent", at = @At("HEAD"), require = 0)
    private void handybell$executeOnRing(int id, int type, CallbackInfoReturnable<Boolean> cir) {
        if (BellHandler.LOCAL_RING) {
            return;
        }
        if (id != BellBlock.EVENT_BELL_RING) {
            return;
        }
        BellBlockEntity self = (BellBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (!(level instanceof ClientLevel)) {
            return;
        }

        HandyBellStore store = HandyBellStore.get();
        BellSpot spot = store.spot(level, self.getBlockPos());
        if (spot == null || !store.isDirect(spot.color)) {
            return;
        }
        // 默认不响应红石/投掷物：钟的响是公共事件，一个拉杆会广播给所有人，
        // 若默认执行，公共据点里任何人随手一动就会把别人的命令一起跑掉。
        // 只能在单人存档里用指令主动打开，见 /handybell allowRedstone。
        if (!store.allowRedstone(spot.color)) {
            return;
        }
        // 局域网已开放、或连的是别人的服务器时，一律不响应。
        // 那些场景下"别人也能触发你的命令"几乎必然变成事故，没有例外可讲。
        if (!local.handybell.core.HandyBellCommand.isTrueSingleplayer()) {
            return;
        }
        SlotConfig only = store.onlySlot(spot.color);
        if (only == null) {
            return;
        }
        CommandRunner.submit(only);
    }
}
