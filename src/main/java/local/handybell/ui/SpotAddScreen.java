package local.handybell.ui;

import local.handybell.core.BellHandler;
import local.handybell.core.Dims;
import local.handybell.store.BellSpot;
import local.handybell.store.HandyBellStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;

/**
 * 新增 / 改色页。
 * <p>
 * 一个界面两种用途，靠 {@code editing} 是否为空区分：新增时要填坐标和颜色，
 * 改色时坐标是既定的、只让它选颜色。合成一处是因为两者的颜色网格、
 * 按钮、返回逻辑完全一样，拆成两个类会有大半是重复代码。
 * <p>
 * 选色用「先选、再确定」而不是「点一下就生效」：改色是低风险操作，
 * 但误点同样烦人，多一次确认换来的是不会手滑把一整组钟换个颜色。
 */
public class SpotAddScreen extends Screen {

    private static final int BTN_H = 20;
    private static final int FIELD_H = 18;
    private static final int DIM_H = 16;
    private static final int DIM_ROW_TOP = 26;
    private static final int GRID_H = 18;
    private static final int GRID_GAP = 6;
    /** 四行网格的总高（含行间缝隙）。 */
    private static final int GRID_BLOCK_H = 4 * GRID_H + 3 * 3;

    private final SpotListScreen parent;
    /** null 表示新增。 */
    private final BellSpot editing;
    private final boolean adding;

    private final EditBox[] xyz = new EditBox[3];
    private final String[] draft = {"", "", ""};
    private final Button[] colorButtons = new Button[16];
    /** 维度按钮：原版三个，当前维度若不在其中就补一个。 */
    private Button[] dimButtons = new Button[0];
    private java.util.List<String> dims = java.util.List.of();

    private String selectedColor;
    private String dimId;
    /** 玩家当前所在的维度。用来判断"取瞄准的钟"能不能用。 */
    private String currentDim;
    private Button aimButton;
    private boolean showHint = true;
    private Component errorMsg;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int colorLabelY;
    private int hintY;

    public SpotAddScreen(SpotListScreen parent, BellSpot editing) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font,
                Component.translatable(editing == null
                        ? "handybell.screen.add.title"
                        : "handybell.screen.add.title_edit"));
        this.parent = parent;
        this.editing = editing;
        this.adding = editing == null;
    }

    @Override
    protected void init() {
        Minecraft mc = Minecraft.getInstance();
        boolean fresh = draft[0].isEmpty() && draft[1].isEmpty() && draft[2].isEmpty();
        currentDim = mc.level == null ? null : mc.level.dimension().identifier().toString();

        if (adding) {
            dimId = currentDim != null ? currentDim : Dims.VANILLA[0];
            selectedColor = HandyBellStore.DEFAULT_COLOR;
            if (fresh) {
                // 默认就把准星指的钟（或脚下）填进去。绝大多数情况下这一填就对了，
                // 玩家只需要挑个颜色。
                BlockPos aim = BellHandler.lookedAtBell();
                if (aim == null && mc.player != null) {
                    aim = mc.player.blockPosition();
                }
                if (aim != null) {
                    draft[0] = String.valueOf(aim.getX());
                    draft[1] = String.valueOf(aim.getY());
                    draft[2] = String.valueOf(aim.getZ());
                }
            }
        } else {
            dimId = editing.dim;
            selectedColor = editing.color;
        }

        panelW = Math.min(this.width - 40, 440);
        panelX = (this.width - panelW) / 2;

        // 先算出这块内容需要多高，再垂直居中。写死 panelY 在 GUI 缩放 4、
        // 1080p（逻辑高度 270）时会从底部溢出，提示文字被裁掉。
        // 屏幕特别矮时先砍掉底部那行提示——它只是说明，控制项不能砍。
        showHint = this.height >= 252;
        int need = DIM_ROW_TOP + DIM_H + 6
                + (adding ? (FIELD_H + 5 + FIELD_H + 8) : 0)
                + 12 + GRID_BLOCK_H + 8 + BTN_H + 8
                + (showHint ? 9 : 0) + 8;
        panelY = Math.max(2, (this.height - need) / 2);
        panelH = need;

        int innerX = panelX + 12;
        int innerW = panelW - 24;

        int y = panelY + DIM_ROW_TOP;
        // 改色时按"这口钟所在的维度"列按钮，否则自定义维度里登记的钟
        // 会一个高亮的都没有。
        dims = Dims.choices(adding ? currentDim : editing.dim);
        dimButtons = new Button[dims.size()];
        int dw = (innerW - (dims.size() - 1) * 6) / dims.size();
        for (int i = 0; i < dims.size(); i++) {
            final String dim = dims.get(i);
            Button btn = Button.builder(dimLabel(dim), b -> this.selectDim(dim))
                    .bounds(innerX + i * (dw + 6), y, dw, DIM_H).build();
            // 改色时维度不可改：坐标本身属于某个维度，"把这口钟挪到另一个维度"
            // 没有意义（那已经是另一个位置了）。置灰比让它点了没反应诚实。
            btn.active = adding;
            dimButtons[i] = btn;
            this.addRenderableWidget(btn);
        }
        y += DIM_H + 6;

        if (adding) {
            int w = (innerW - 12) / 3;
            for (int i = 0; i < 3; i++) {
                final int k = i;
                EditBox box = new EditBox(this.font, innerX + i * (w + 6), y, w, FIELD_H,
                        Component.literal("xyz".substring(i, i + 1)));
                box.setMaxLength(12);
                box.setValue(draft[i]);
                box.setResponder(s -> {
                    draft[k] = s;
                    errorMsg = null;
                });
                this.addRenderableWidget(box);
                xyz[i] = box;
            }
            y += FIELD_H + 5;

            aimButton = Button.builder(
                            Component.translatable("handybell.screen.add.aim"), b -> this.aim())
                    .bounds(innerX, y, innerW, FIELD_H).build();
            // 准星只能指着当前维度的东西，选了别的维度这个按钮就没有意义
            aimButton.active = isCurrentDim();
            this.addRenderableWidget(aimButton);
            y += FIELD_H + 8;
        }

        colorLabelY = y;
        y += 12;

        int cw = (innerW - 3 * GRID_GAP) / 4;
        for (int i = 0; i < HandyBellStore.COLORS.length; i++) {
            final String color = HandyBellStore.COLORS[i];
            int col = i % 4;
            int row = i / 4;
            Button btn = Button.builder(colorLabel(color), b -> this.select(color))
                    .bounds(innerX + col * (cw + GRID_GAP), y + row * (GRID_H + 3), cw, GRID_H)
                    .build();
            colorButtons[i] = btn;
            this.addRenderableWidget(btn);
        }
        y += GRID_BLOCK_H + 8;

        int totalW = 96 * 2 + 12;
        int bx = (this.width - totalW) / 2;
        this.addRenderableWidget(Button.builder(
                        Component.translatable("handybell.screen.add.confirm"), b -> this.confirm())
                .bounds(bx, y, 96, BTN_H).build());
        this.addRenderableWidget(Button.builder(
                        Component.translatable("handybell.screen.add.cancel"),
                        b -> Minecraft.getInstance().setScreenAndShow(parent))
                .bounds(bx + 108, y, 96, BTN_H).build());

        hintY = y + BTN_H + 8;
    }

    private boolean isCurrentDim() {
        return currentDim != null && currentDim.equals(dimId);
    }

    /** 选中的维度前面加个箭头，和颜色网格用同一套标记。 */
    private Component dimLabel(String dim) {
        Component name = Dims.name(dim);
        return dim.equals(dimId)
                ? Component.empty().append(Component.literal("▶ ")).append(name)
                : name;
    }

    private void selectDim(String dim) {
        dimId = dim;
        errorMsg = null;
        for (int i = 0; i < dimButtons.length; i++) {
            if (dimButtons[i] != null) {
                dimButtons[i].setMessage(dimLabel(dims.get(i)));
            }
        }
        if (aimButton != null) {
            aimButton.active = isCurrentDim();
        }
    }

    /** 选中的那格前面加个箭头。没有 setter 能改按钮的"按下"状态，改文字最省事。 */
    private Component colorLabel(String color) {
        Component name = ColorNames.of(color);
        return color.equals(selectedColor)
                ? Component.empty().append(Component.literal("▶ ")).append(name)
                : name;
    }

    private void select(String color) {
        selectedColor = color;
        for (int i = 0; i < colorButtons.length; i++) {
            if (colorButtons[i] != null) {
                colorButtons[i].setMessage(colorLabel(HandyBellStore.COLORS[i]));
            }
        }
    }

    private void aim() {
        BlockPos pos = BellHandler.lookedAtBell();
        if (pos == null) {
            errorMsg = Component.translatable("handybell.cmd.add_no_target");
            return;
        }
        errorMsg = null;
        draft[0] = String.valueOf(pos.getX());
        draft[1] = String.valueOf(pos.getY());
        draft[2] = String.valueOf(pos.getZ());
        for (int i = 0; i < 3; i++) {
            if (xyz[i] != null) {
                xyz[i].setValue(draft[i]);
            }
        }
    }

    private void confirm() {
        if (!adding) {
            HandyBellStore.get().setColorByKey(editing.key(), selectedColor);
            Minecraft.getInstance().setScreenAndShow(parent);
            return;
        }

        int x;
        int y;
        int z;
        try {
            x = Integer.parseInt(draft[0].trim());
            y = Integer.parseInt(draft[1].trim());
            z = Integer.parseInt(draft[2].trim());
        } catch (NumberFormatException e) {
            errorMsg = Component.translatable("handybell.cmd.add_bad_coords",
                    draft[0] + " " + draft[1] + " " + draft[2]);
            return;
        }

        BlockPos pos = new BlockPos(x, y, z);
        Minecraft mc = Minecraft.getInstance();
        boolean here = isCurrentDim();

        // 只有同维度才查得了方块；给别的维度登记时没法验证，只能照单收下
        if (here && mc.level != null && mc.level.hasChunkAt(pos)
                && !mc.level.getBlockState(pos).is(Blocks.BELL)) {
            errorMsg = Component.translatable("handybell.cmd.add_not_bell",
                    Dims.where(dimId, x, y, z));
            return;
        }

        HandyBellStore store = HandyBellStore.get();
        BellSpot existing = store.byKey(BellSpot.key(dimId, x, y, z));
        if (existing != null) {
            existing.color = selectedColor;
            HandyBellStore.save();
        } else if (here && mc.level != null) {
            store.addSpot(mc.level, pos, selectedColor);
        } else {
            store.addSpotIn(dimId, x, y, z, selectedColor);
        }
        Minecraft.getInstance().setScreenAndShow(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        GuiHelper.panel(g, panelX, panelY, panelW, panelH, 0xE8101014, 0x60FFFFFF);
        super.extractRenderState(g, mouseX, mouseY, partialTick);

        int cx = panelX + panelW / 2;
        Component title = adding
                ? Component.translatable("handybell.screen.add.title")
                : Component.translatable("handybell.screen.add.title_edit", ColorNames.of(editing.color));
        g.centeredText(this.font, title, cx, panelY + 10, 0xFFFFE0A0);

        g.text(this.font, Component.translatable("handybell.screen.add.color"),
                panelX + 12, colorLabelY, 0xFF9A938C);

        if (errorMsg != null) {
            // 报错顶掉下面的提示，不并列——否则窄面板上两行文字会撞在一起
            g.centeredText(this.font, errorMsg, cx, hintY, 0xFFFF7B72);
        } else if (showHint) {
            g.centeredText(this.font, Component.translatable("handybell.screen.add.hint"),
                    cx, hintY, 0xFF7E786F);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
