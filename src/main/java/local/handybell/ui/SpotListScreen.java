package local.handybell.ui;

import local.handybell.core.Dims;
import local.handybell.store.BellSpot;
import local.handybell.store.HandyBellStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/**
 * 名单管理页：列出所有登记过的钟，可以改色、删除、新增。
 * <p>
 * 存在的理由：原来的登记只有"把钟命名为给事钟、再放下"这一条路。
 * 在限制放置方块的服务器上（区域保护、领地插件），玩家根本放不下钟，
 * 于是这个模组在那类服务器上完全没法用。有了这一页，
 * 玩家只要"看着"已经存在的钟就能把它登记进来。
 * <p>
 * 用分页而不是滚动条：Minecraft 的 Screen 没有现成的滚动容器，
 * 自己做滚动要处理裁剪和事件穿透，分页用几行代码就能达到同样的效果。
 */
public class SpotListScreen extends Screen {

    private static final int ROW_H = 20;
    private static final int ROW_GAP = 4;
    private static final int COLOR_W = 92;
    /** 删除按钮用文字而不是符号，宽度得放得下俄语、德语这类长词。 */
    private static final int DEL_W = 72;

    /** 进入时的快照。删掉一条之后会重建控件，届时重新取。 */
    private List<BellSpot> rows = new ArrayList<>();
    private int page;
    private int perPage = 8;
    private int pages = 1;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int listTop;

    public SpotListScreen() {
        super(Minecraft.getInstance(), Minecraft.getInstance().font,
                Component.translatable("handybell.screen.manage.title"));
    }

    @Override
    protected void init() {
        rows = HandyBellStore.get().allSpots();

        perPage = Math.max(3, (this.height - 168) / (ROW_H + ROW_GAP));
        pages = Math.max(1, (rows.size() + perPage - 1) / perPage);
        page = Math.max(0, Math.min(page, pages - 1));

        panelW = Math.min(this.width - 32, 520);
        panelX = (this.width - panelW) / 2;
        panelY = 30;

        int innerX = panelX + 10;
        int innerW = panelW - 20;
        listTop = panelY + 26;

        for (int i = 0; i < perPage; i++) {
            int idx = page * perPage + i;
            if (idx >= rows.size()) {
                break;
            }
            BellSpot spot = rows.get(idx);
            int y = listTop + i * (ROW_H + ROW_GAP);
            int delX = innerX + innerW - DEL_W;
            int colorX = delX - 6 - COLOR_W;

            // 按钮直接显示当前颜色名：既说明它是哪一组，也顺便当"改色"的入口。
            this.addRenderableWidget(Button.builder(ColorNames.of(spot.color),
                            b -> Minecraft.getInstance().setScreenAndShow(new SpotAddScreen(this, spot)))
                    .bounds(colorX, y, COLOR_W, ROW_H).build());

            // 用文字而不是 ✘：右边状态列里已经用 ✘ 表示"那儿不是钟"，
            // 同一个符号既当状态又当操作按钮，很容易看成"这行有问题，点它修"。
            this.addRenderableWidget(Button.builder(
                    Component.translatable("handybell.screen.manage.delete"), b -> {
                        HandyBellStore.get().removeByKey(spot.key());
                        this.rebuildWidgets();
                    }).bounds(delX, y, DEL_W, ROW_H).build());
        }

        int y = listTop + perPage * (ROW_H + ROW_GAP) + 2;

        this.addRenderableWidget(Button.builder(
                        Component.translatable("handybell.screen.manage.add"),
                        b -> Minecraft.getInstance().setScreenAndShow(new SpotAddScreen(this, null)))
                .bounds(innerX, y, 84, 20).build());

        if (pages > 1) {
            Button prev = Button.builder(Component.translatable("handybell.screen.macro.prev"), b -> {
                if (page > 0) {
                    page--;
                    this.rebuildWidgets();
                }
            }).bounds(innerX + innerW - 136, y, 64, 20).build();
            prev.active = page > 0;
            this.addRenderableWidget(prev);

            Button next = Button.builder(Component.translatable("handybell.screen.macro.next"), b -> {
                if (page < pages - 1) {
                    page++;
                    this.rebuildWidgets();
                }
            }).bounds(innerX + innerW - 68, y, 64, 20).build();
            next.active = page < pages - 1;
            this.addRenderableWidget(next);
        }

        y += 26;
        this.addRenderableWidget(Button.builder(
                        Component.translatable("handybell.screen.manage.close"), b -> this.onClose())
                .bounds((this.width - 84) / 2, y, 84, 20).build());

        panelH = (y + 20 + 10) - panelY;
    }

    /**
     * ✔ 是钟　✘ 不是钟　? 判断不了
     * <p>
     * 「判断不了」有两种：区块没加载，或者那口钟在别的维度。
     * 两种情况都没法查方块，所以合并成一个问号，不细分——细分了玩家也做不了什么。
     */
    private static String statusOf(BellSpot spot) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return "?";
        }
        if (!mc.level.dimension().identifier().toString().equals(spot.dim)) {
            return "?";
        }
        BlockPos pos = new BlockPos(spot.x, spot.y, spot.z);
        if (!mc.level.hasChunkAt(pos)) {
            return "?";
        }
        return mc.level.getBlockState(pos).is(Blocks.BELL) ? "✔" : "✘";
    }

    private static int statusColor(String status) {
        return switch (status) {
            case "✔" -> 0xFF7BD88F;
            case "✘" -> 0xFFFF7B72;
            default -> 0xFF8A8378;
        };
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        GuiHelper.panel(g, panelX, panelY, panelW, panelH, 0xE8101014, 0x60FFFFFF);
        super.extractRenderState(g, mouseX, mouseY, partialTick);

        g.centeredText(this.font,
                Component.translatable("handybell.screen.manage.title_with_page", page + 1, pages),
                panelX + panelW / 2, panelY + 9, 0xFFFFE0A0);

        if (rows.isEmpty()) {
            g.centeredText(this.font, Component.translatable("handybell.screen.manage.empty"),
                    panelX + panelW / 2, panelY + 60, 0xFFB4B2A9);
            g.centeredText(this.font, Component.translatable("handybell.screen.manage.empty_hint"),
                    panelX + panelW / 2, panelY + 76, 0xFF888780);
            return;
        }

        int innerX = panelX + 10;
        int innerW = panelW - 20;
        int textLimit = innerX + innerW - COLOR_W - DEL_W - 14;

        for (int i = 0; i < perPage; i++) {
            int idx = page * perPage + i;
            if (idx >= rows.size()) {
                break;
            }
            BellSpot spot = rows.get(idx);
            int y = listTop + i * (ROW_H + ROW_GAP) + 6;

            String status = statusOf(spot);
            g.text(this.font, Component.literal(status), innerX + 2, y, statusColor(status));

            // 维度在前、坐标在后并加括号，和 /handybell add 的读法一致。
            // 自定义维度形如 mypack:mining，不套括号会和数字连读成一片。
            Component dim = Dims.name(spot.dim);
            Component coords = Component.literal(
                    "(" + spot.x + ", " + spot.y + ", " + spot.z + ")");
            int nx = innerX + 14;
            int dimW = this.font.width(dim);
            // 放不下就先丢掉维度名，坐标留着——定位靠的是坐标
            if (nx + dimW + 6 + this.font.width(coords) <= textLimit) {
                g.text(this.font, dim, nx, y, 0xFF9A938C);
                nx += dimW + 6;
            }
            g.text(this.font, coords, nx, y, 0xFFE6E1DC);
        }

        g.text(this.font, Component.translatable("handybell.screen.manage.legend"),
                innerX + 2, panelH + panelY - 62, 0xFF7E786F);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
