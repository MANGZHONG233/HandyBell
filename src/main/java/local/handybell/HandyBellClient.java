package local.handybell;

import local.handybell.compat.CommandKeysBridge;
import local.handybell.core.BellHandler;
import local.handybell.core.IBellColorState;
import local.handybell.store.HandyBellStore;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.state.BellRenderState;

public class HandyBellClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 启动时先把配置和反射预热好，避免第一次敲钟时才去读硬盘/反射，导致卡一帧黑屏
        HandyBellStore.get();
        boolean ck = CommandKeysBridge.available();
        int macros = ck ? CommandKeysBridge.macroNames().size() : 0;

        // 自检：渲染状态上的 mixin 有没有真的挂上去
        boolean colorMixin = IBellColorState.class.isAssignableFrom(BellRenderState.class);

        BellHandler.LOGGER.info("HandyBell 已加载 (Command Keys 联动: {})",
                ck && CommandKeysBridge.usable() ? "开启" : "关闭");
    }
}
