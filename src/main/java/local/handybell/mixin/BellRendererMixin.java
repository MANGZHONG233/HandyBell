package local.handybell.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import local.handybell.core.IBellColorState;
import local.handybell.store.BellSpot;
import local.handybell.store.HandyBellStore;
import net.minecraft.client.model.object.bell.BellModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BellRenderer;
import net.minecraft.client.renderer.blockentity.state.BellRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * 给事钟染色。
 * <p>
 * 一开始想用顶点色 tint，但铜色贴图的蓝通道上限只有 74，
 * 乘算永远做不出粉/蓝/紫这类颜色。所以改成按颜色换整张贴图：
 * 自带 16 张 bell_body_<color>.png，用 RenderTypes.entityCutout(独立纹理) 直接绑定，
 * 不走 atlas，也不会和任何材质包冲突。
 */
@Mixin(BellRenderer.class)
public class BellRendererMixin {

    /** 贴图是独立文件，UV 就是 0~1 原样，不需要 atlas 的偏移映射。 */
    private static final UvMapping IDENTITY_UV = new UvMapping() {
        @Override
        public float getU(float u) {
            return u;
        }

        @Override
        public float getV(float v) {
            return v;
        }
    };

    private static final Map<String, RenderType> TYPE_CACHE = new HashMap<>();

    @Shadow
    @Final
    private BellModel model;

    @Shadow
    @Final
    private SpriteGetter sprites;

    private static RenderType typeFor(String color) {
        return TYPE_CACHE.computeIfAbsent(color, c -> RenderTypes.entityCutout(
                Identifier.fromNamespaceAndPath("handybell",
                        "textures/entity/bell/bell_body_" + c + ".png")));
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"), require = 0)
    private void handybell$extract(BellBlockEntity be, BellRenderState state, float partialTick,
                                   Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay overlay,
                                   CallbackInfo ci) {
        if (be == null || state == null) {
            return;
        }
        Level level = be.getLevel();
        if (level == null) {
            return;
        }
        BellSpot spot = HandyBellStore.get().spot(level, be.getBlockPos());
        int index = spot == null ? 0 : (HandyBellStore.indexOf(spot.color) + 1);
        ((IBellColorState) state).handybell$color(index);
    }

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true, require = 0)
    private void handybell$submit(BellRenderState state, PoseStack poseStack,
                                  SubmitNodeCollector collector, CameraRenderState cameraState,
                                  CallbackInfo ci) {
        if (state == null || collector == null) {
            return;
        }
        int index = ((IBellColorState) state).handybell$color();
        if (index <= 0 || index > HandyBellStore.COLORS.length) {
            return;
        }

        String color = HandyBellStore.COLORS[index - 1];
        BellModel.State animState = new BellModel.State(state.ticks, state.shakeDirection);
        this.model.setupAnim(animState);
        collector.submitModel(this.model, animState, poseStack,
                typeFor(color),
                state.lightCoords, OverlayTexture.NO_OVERLAY, -1,
                IDENTITY_UV, 0);

        ci.cancel();
    }
}
