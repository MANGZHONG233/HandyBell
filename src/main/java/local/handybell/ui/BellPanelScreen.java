package local.handybell.ui;

import local.handybell.core.CommandRunner;
import local.handybell.store.BellSpot;
import local.handybell.store.HandyBellStore;
import local.handybell.store.SlotConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** 敲响给事钟后弹出的九宫格操作面板。 */
public class BellPanelScreen extends Screen {

    private static final int BTN_H = 24;
    private static final int GAP = 8;

    private final BellSpot spot;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    public BellPanelScreen(BellSpot spot) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.translatable("handybell.screen.panel.title"));
        this.spot = spot;
    }

    @Override
    protected void init() {
        List<SlotConfig> slots = HandyBellStore.get().group(spot.color);

        int btnW = Math.min(150, Math.max(90, (this.width - 56) / 3));
        int totalW = btnW * 3 + GAP * 2;
        int x0 = (this.width - totalW) / 2;
        int y0 = Math.max(44, this.height / 2 - 44);
        int btnAreaH = 3 * BTN_H + 2 * GAP;

        panelX = x0 - 14;
        panelY = y0 - 28;
        panelW = totalW + 28;
        panelH = btnAreaH + 96;

        for (int i = 0; i < HandyBellStore.SLOT_COUNT; i++) {
            int col = i % 3;
            int row = i / 3;
            SlotConfig cfg = slots.get(i);
            String text = cfg.isEmpty() ? "—" : trim(cfg.label(), 15);
            Button btn = Button.builder(Component.literal((i + 1) + ". " + text), b -> {
                if (!cfg.isEmpty()) {
                    CommandRunner.submit(cfg);
                    this.onClose();
                }
            }).bounds(x0 + col * (btnW + GAP), y0 + row * (BTN_H + GAP), btnW, BTN_H).build();
            btn.active = !cfg.isEmpty();
            this.addRenderableWidget(btn);
        }

        int closeY = y0 + btnAreaH + 14;
        this.addRenderableWidget(Button.builder(Component.translatable("handybell.screen.panel.close"), b -> this.onClose())
                .bounds((this.width - 100) / 2, closeY, 100, 20).build());
        panelH = (closeY + 20 + 12) - panelY;
    }

    private static String trim(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        GuiHelper.panel(g, panelX, panelY, panelW, panelH, 0xE8101014, 0x60FFFFFF);
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.centeredText(this.font, Component.translatable("handybell.screen.panel.title_with_color", ColorNames.of(spot.color)),
                panelX + panelW / 2, panelY + 14, 0xFFFFE0A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
