package local.handybell.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** 绘图小工具。这里只用 fill 拼边框，避免 outline 参数含义在不同版本里不一致。 */
public final class GuiHelper {

    private GuiHelper() {
    }

    /** 用四条 1px 实心线画一个矩形边框（x,y 为左上角，w/h 为尺寸）。 */
    public static void border(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h, int fillColor, int borderColor) {
        g.fill(x, y, x + w, y + h, fillColor);
        border(g, x, y, w, h, borderColor);
    }
}
