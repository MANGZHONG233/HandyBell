package local.handybell.ui;

import local.handybell.core.HandyBellCommand;
import local.handybell.store.HandyBellStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * "允许红石触发"的二次确认页。
 * <p>
 * 这个设置会让人在你不知情的情况下远程触发你的命令，所以不放进配置界面，
 * 只能由 {@code /handybell allowRedstone <颜色> true} 唤起，并且必须在这里点过确认才生效。
 */
public class AllowRedstoneScreen extends Screen {

    private final String color;
    private int panelX;
    private int panelY;
    private int panelW = 420;
    private int panelH = 176;

    public AllowRedstoneScreen(String color, Object ignored) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font,
                Component.translatable("handybell.warn.title"));
        this.color = color;
    }

    @Override
    protected void init() {
        panelW = Math.min(this.width - 40, 460);
        panelH = 186;
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        int btnW = 170;
        int gap = 12;
        int totalW = btnW * 2 + gap;
        int bx = (this.width - totalW) / 2;
        int by = panelY + panelH - 34;

        this.addRenderableWidget(Button.builder(
                        Component.translatable("handybell.warn.confirm"), b -> this.confirm())
                .bounds(bx, by, btnW, 20).build());
        this.addRenderableWidget(Button.builder(
                        Component.translatable("handybell.warn.cancel"), b -> this.onClose())
                .bounds(bx + btnW + gap, by, btnW, 20).build());
    }

    private void confirm() {
        HandyBellStore.get().setAllowRedstone(color, true);
        HandyBellStore.save();
        this.onClose();
        HandyBellCommand.message(Component.translatable("handybell.cmd.redstone_on", color));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        GuiHelper.panel(g, panelX, panelY, panelW, panelH, 0xE81A1014, 0x80FF6B6B);
        super.extractRenderState(g, mouseX, mouseY, partialTick);

        int cx = panelX + panelW / 2;
        g.centeredText(this.font, Component.translatable("handybell.warn.title"),
                cx, panelY + 14, 0xFFFF7B72);

        int y = panelY + 38;
        for (String key : new String[]{
                "handybell.warn.body1", "handybell.warn.body2", "handybell.warn.body3"}) {
            g.centeredText(this.font, Component.translatable(key), cx, y, 0xFFD8D2CA);
            y += 15;
        }

        g.centeredText(this.font,
                Component.translatable("handybell.warn.color", color),
                cx, panelY + panelH - 52, 0xFF9A938C);

        g.centeredText(this.font, Component.translatable("handybell.warn.confirm_hint"),
                cx, panelY + panelH - 68, 0xFF8A8378);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
