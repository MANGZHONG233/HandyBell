package local.handybell.mixin;

import local.handybell.core.IBellColorState;
import net.minecraft.client.renderer.blockentity.state.BellRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** 给钟的渲染状态挂上“这口钟该染什么色”。0 表示不染色。 */
@Mixin(BellRenderState.class)
public abstract class BellRenderStateMixin implements IBellColorState {

    @Unique
    private int handybell$color = 0;

    @Override
    public int handybell$color() {
        return this.handybell$color;
    }

    @Override
    public void handybell$color(int color) {
        this.handybell$color = color;
    }
}
