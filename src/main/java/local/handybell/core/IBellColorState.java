package local.handybell.core;

/** 给 BellRenderState 挂一个颜色字段用的接口（跨 mixin 读写）。 */
public interface IBellColorState {

    int handybell$color();

    void handybell$color(int color);
}
