package local.handybell.ui;

import local.handybell.compat.CommandKeysBridge;
import local.handybell.core.Notices;
import local.handybell.store.BellSpot;
import local.handybell.store.HandyBellStore;
import local.handybell.store.SlotConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** 配置页：给当前颜色分组的 9 个槽位绑定指令集/命令与描述。 */
public class BellConfigScreen extends Screen {

    /**
     * 从哪口钟进来的。
     * <p>
     * 可能为 null——{@code /handybell config <颜色>} 是直接按颜色组打开的，
     * 那会儿手上没有具体某口钟。为 null 时只是不显示「取消登记」，
     * 其余行为完全一样（配置本来是按颜色组存的，跟钟的坐标无关）。
     */
    private final BellSpot spot;
    private final String color;
    private final EditBox[] cmdBoxes = new EditBox[HandyBellStore.SLOT_COUNT];
    private final EditBox[] descBoxes = new EditBox[HandyBellStore.SLOT_COUNT];
    private final String[] draftCmd = new String[HandyBellStore.SLOT_COUNT];
    private final String[] draftDesc = new String[HandyBellStore.SLOT_COUNT];
    private boolean hasDraft;
    private boolean directDraft;
    /** 复选框要随时按草稿刷新可用性，所以留个引用。 */
    private Checkbox directBox;

    private int rowTop;
    private int rowH;
    private int labelX;
    private int textX;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    public BellConfigScreen(BellSpot spot) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.translatable("handybell.screen.config.title"));
        this.spot = spot;
        this.color = spot.color;
    }

    /** 按颜色组直接打开，不关联任何一口钟。 */
    public BellConfigScreen(String color) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.translatable("handybell.screen.config.title"));
        this.spot = null;
        this.color = color;
    }

    @Override
    protected void init() {
        HandyBellStore store = HandyBellStore.get();
        List<SlotConfig> slots = store.group(color);
        if (!hasDraft) {
            for (int i = 0; i < HandyBellStore.SLOT_COUNT; i++) {
                SlotConfig cfg = slots.get(i);
                draftCmd[i] = cfg.macro.isBlank() ? cfg.command : ("@" + cfg.macro);
                draftDesc[i] = cfg.desc;
            }
            directDraft = store.isDirect(color);
            hasDraft = true;
        }

        boolean hasKeys = CommandKeysBridge.available();

        // 纵向：标题 40 + 9 行 + 复选 26 + 按钮 34
        rowTop = 40;
        rowH = Math.max(15, Math.min(22, (this.height - 100) / HandyBellStore.SLOT_COUNT));

        int pickW = hasKeys ? 28 : 0;
        int pickGap = hasKeys ? 6 : 0;
        int labelW = 20;
        int gap1 = 8;
        int maxW = Math.min(this.width - 32, 620);
        int cmdW = Math.max(110, (maxW - labelW - pickW - pickGap - gap1) * 56 / 100);
        int descW = maxW - labelW - pickW - pickGap - gap1 - cmdW;

        panelW = maxW + 24;
        panelX = (this.width - panelW) / 2;
        labelX = panelX + 12;
        textX = labelX + labelW + pickW + pickGap;

        for (int i = 0; i < HandyBellStore.SLOT_COUNT; i++) {
            int y = rowTop + i * rowH;
            int x = textX;

            if (hasKeys) {
                final int slotIndex = i;
                this.addRenderableWidget(Button.builder(Component.translatable("handybell.screen.config.pick"),
                                b -> this.openPicker(slotIndex))
                        .bounds(labelX + labelW, y, pickW, Math.max(14, rowH - 3)).build());
            }

            EditBox cmd = new EditBox(this.font, x, y, cmdW, Math.max(14, rowH - 3), Component.translatable("handybell.screen.config.cmd"));
            cmd.setMaxLength(256);
            cmd.setValue(draftCmd[i]);
            cmd.setHint(Component.translatable(hasKeys
                    ? "handybell.screen.config.hint.macro"
                    : "handybell.screen.config.hint.plain"));
            this.addRenderableWidget(cmd);
            cmdBoxes[i] = cmd;
            // 输入变化时实时重算复选框可用性，否则改完条数还得先保存重开才能勾
            cmd.setResponder(text -> refreshDirectState());

            EditBox desc = new EditBox(this.font, x + cmdW + gap1, y, descW, Math.max(14, rowH - 3),
                    Component.translatable("handybell.screen.config.desc"));
            desc.setMaxLength(64);
            desc.setValue(draftDesc[i]);
            desc.setHint(Component.translatable("handybell.screen.config.desc_hint"));
            this.addRenderableWidget(desc);
            descBoxes[i] = desc;
        }

        int afterRows = rowTop + HandyBellStore.SLOT_COUNT * rowH;

        Checkbox direct = Checkbox.builder(Component.translatable("handybell.screen.config.direct"), this.font)
                .pos(labelX, afterRows + 4)
                .selected(directDraft)
                .maxWidth(Math.min(360, panelW - 24))
                .onValueChange((box, value) -> directDraft = value)
                .build();
        directBox = direct;
        this.addRenderableWidget(direct);

        // 注意：这里刻意没有"允许红石触发"的开关。
        // 那个设置会让人在不知情的情况下被人远程触发命令，所以只通过指令
        // /handybell allowRedstone <颜色> true 开启，并且要过一道二次确认。
        refreshDirectState();

        int btnY = afterRows + 28;
        int btnW = 96;
        int btnGap = 6;
        // 没有具体某口钟时不显示「取消登记」——没有可取消的对象。
        // 按钮组按实际个数居中，否则少一个会偏左。
        int count = spot == null ? 2 : 3;
        int totalW = btnW * count + btnGap * (count - 1);
        int bx = (this.width - totalW) / 2;

        this.addRenderableWidget(Button.builder(Component.translatable("handybell.screen.config.save"), b -> {
            this.save();
            this.onClose();
        }).bounds(bx, btnY, btnW, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("handybell.screen.config.cancel"), b -> this.onClose())
                .bounds(bx + btnW + btnGap, btnY, btnW, 20).build());

        if (spot != null) {
            this.addRenderableWidget(Button.builder(Component.translatable("handybell.screen.config.unregister"), b -> this.unregister())
                    .bounds(bx + (btnW + btnGap) * 2, btnY, btnW, 20).build());
        }

        panelY = rowTop - 32;
        panelH = (btnY + 20 + 10) - panelY;
    }

    private void capture() {
        for (int i = 0; i < HandyBellStore.SLOT_COUNT; i++) {
            if (cmdBoxes[i] != null) {
                draftCmd[i] = cmdBoxes[i].getValue();
            }
            if (descBoxes[i] != null) {
                draftDesc[i] = descBoxes[i].getValue();
            }
        }
        hasDraft = true;
    }

    private void openPicker(int slotIndex) {
        this.capture();
        Minecraft.getInstance().setScreenAndShow(new MacroPickScreen(this, slotIndex));
    }

    /** 由 MacroPickScreen 回调：把选中的指令集写进草稿（带 @ 前缀标识这是指令集）。 */
    public void applyMacro(int slotIndex, String macroName) {
        if (slotIndex >= 0 && slotIndex < HandyBellStore.SLOT_COUNT) {
            draftCmd[slotIndex] = "@" + macroName;
            hasDraft = true;
        }
    }

    /** 把这口钟从名单里摘掉：钟还留着，但恢复成普通钟的行为。 */
    private void unregister() {
        if (spot == null) {
            return;
        }
        var level = Minecraft.getInstance().level;
        if (level != null) {
            HandyBellStore.get().remove(level, new net.minecraft.core.BlockPos(spot.x, spot.y, spot.z));
            Notices.bellUnregistered(spot);
        }
        this.onClose();
    }

    /**
     * 按当前输入框内容重算"直接触发"是否可用。
     * <p>
     * 只允许在某组恰好有且仅有一条内容时勾选。之前用的是已保存的数据，
     * 导致改完条数必须先保存、关掉再打开才能勾——这里改成实时跟随草稿。
     * <p>
     * 刻意不去清掉已有的勾选：条数临时变成 2 条时它只是变灰不可点，
     * 改回 1 条又会亮起来并保留原勾选状态，比强制取消更顺手。
     */
    private void refreshDirectState() {
        if (this.directBox == null) {
            return;
        }
        int filled = 0;
        for (EditBox box : this.cmdBoxes) {
            if (box != null && !box.getValue().trim().isEmpty()) {
                filled++;
            }
        }
        this.directBox.active = filled == 1;
    }

    private void save() {
        capture();
        HandyBellStore store = HandyBellStore.get();
        List<SlotConfig> slots = store.group(color);
        for (int i = 0; i < HandyBellStore.SLOT_COUNT; i++) {
            SlotConfig cfg = slots.get(i);
            String value = draftCmd[i] == null ? "" : draftCmd[i].trim();
            cfg.desc = draftDesc[i] == null ? "" : draftDesc[i].trim();
            if (value.isEmpty()) {
                cfg.macro = "";
                cfg.command = "";
            } else if (value.startsWith("@")) {
                cfg.macro = value.substring(1).trim();
                cfg.command = "";
            } else {
                cfg.macro = "";
                cfg.command = value;
            }
        }
        // 保存时把勾选状态规范化：条数不是恰好一条就存 false，
        // 免得配置文件里留下一个永远不会生效的 true。
        store.setDirect(color, directDraft && store.filledCount(color) == 1);
        HandyBellStore.save();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        GuiHelper.panel(g, panelX, panelY, panelW, panelH, 0xE8101014, 0x60FFFFFF);
        super.extractRenderState(g, mouseX, mouseY, partialTick);

        g.centeredText(this.font,
                Component.translatable("handybell.screen.config.title_with_color", ColorNames.of(color)),
                panelX + panelW / 2, panelY + 11, 0xFFFFE0A0);

        int headY = rowTop - 16;
        g.text(this.font, Component.literal("#"), labelX, headY, 0xFF9A938C);
        g.text(this.font,
                Component.translatable(CommandKeysBridge.available()
                        ? "handybell.screen.config.col_cmd"
                        : "handybell.screen.config.col_cmd_plain"),
                textX, headY, 0xFF9A938C);

        for (int i = 0; i < HandyBellStore.SLOT_COUNT; i++) {
            g.text(this.font, Component.literal(String.valueOf(i + 1)),
                    labelX + 4, rowTop + i * rowH + 4, 0xFFE6E1DC);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
