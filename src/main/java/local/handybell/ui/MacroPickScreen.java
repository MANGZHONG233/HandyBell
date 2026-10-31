package local.handybell.ui;

import local.handybell.compat.CommandKeysBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** 从 Command Keys 当前 profile 里挑一个指令集。 */
public class MacroPickScreen extends Screen {

    private static final int BTN_H = 20;
    private static final int GAP = 4;

    private final BellConfigScreen parent;
    private final int slotIndex;
    private final List<String> names;
    private int page;
    private int perPage;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    public MacroPickScreen(BellConfigScreen parent, int slotIndex) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.translatable("handybell.screen.macro.title"));
        this.parent = parent;
        this.slotIndex = slotIndex;
        this.names = CommandKeysBridge.macroNames();
    }

    @Override
    protected void init() {
        perPage = Math.max(4, (this.height - 150) / (BTN_H + GAP));
        panelW = Math.min(300, this.width - 40);
        panelX = (this.width - panelW) / 2;
        panelY = 30;

        int x = panelX + 10;
        int innerW = panelW - 20;
        int y0 = panelY + 26;
        int start = page * perPage;

        for (int i = 0; i < perPage; i++) {
            int idx = start + i;
            if (idx >= names.size()) {
                break;
            }
            String key = names.get(idx);
            String label = CommandKeysBridge.displayName(key);
            this.addRenderableWidget(Button.builder(Component.literal(label), b -> {
                parent.applyMacro(slotIndex, key);
                Minecraft.getInstance().setScreenAndShow(parent);
            }).bounds(x, y0 + i * (BTN_H + GAP), innerW, BTN_H).build());
        }

        int pages = Math.max(1, (names.size() + perPage - 1) / perPage);
        int navY = y0 + perPage * (BTN_H + GAP) + 2;

        if (pages > 1) {
            Button prev = Button.builder(Component.translatable("handybell.screen.macro.prev"), b -> {
                if (page > 0) {
                    page--;
                    this.rebuildWidgets();
                }
            }).bounds(x, navY, 70, 20).build();
            prev.active = page > 0;
            this.addRenderableWidget(prev);

            Button next = Button.builder(Component.translatable("handybell.screen.macro.next"), b -> {
                if (page < pages - 1) {
                    page++;
                    this.rebuildWidgets();
                }
            }).bounds(x + innerW - 70, navY, 70, 20).build();
            next.active = page < pages - 1;
            this.addRenderableWidget(next);

            navY += 24;
        }

        this.addRenderableWidget(Button.builder(Component.translatable("handybell.screen.macro.back"),
                        b -> Minecraft.getInstance().setScreenAndShow(parent))
                .bounds((this.width - 90) / 2, navY, 90, 20).build());

        panelH = (navY + 20 + 10) - panelY;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        GuiHelper.panel(g, panelX, panelY, panelW, panelH, 0xE8101014, 0x60FFFFFF);
        super.extractRenderState(g, mouseX, mouseY, partialTick);

        g.centeredText(this.font, Component.translatable("handybell.screen.macro.title_with_slot", slotIndex + 1),
                panelX + panelW / 2, panelY + 11, 0xFFFFE0A0);
        if (names.isEmpty()) {
            g.centeredText(this.font, Component.translatable("handybell.screen.macro.empty"),
                    panelX + panelW / 2, panelY + 60, 0xFFB4B2A9);
            g.centeredText(this.font, Component.translatable("handybell.screen.macro.empty_hint"),
                    panelX + panelW / 2, panelY + 76, 0xFF888780);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
