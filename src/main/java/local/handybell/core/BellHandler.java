package local.handybell.core;

import local.handybell.store.BellSpot;
import local.handybell.store.HandyBellStore;
import local.handybell.store.SlotConfig;
import local.handybell.ui.BellConfigScreen;
import local.handybell.ui.BellPanelScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 钟的交互判定与本地表现。 */
public final class BellHandler {

    public static final Logger LOGGER = LoggerFactory.getLogger("handybell");

    /**
     * 为 true 表示当前正由"玩家自己右键"触发本地响铃。
     * 客户端监听 blockEvent 时要跳过这一种，否则右键会被当成红石触发、命令执行两遍。
     */
    public static volatile boolean LOCAL_RING = false;

    private static final String NAME_ZH = "给事钟";
    private static final String NAME_ZH_TW = "給事鐘";
    private static final String NAME_EN = "HandyBell";

    /**
     * 放置前手里那口钟的副本。
     * 敲下去之后手上可能已经空了（只剩一个的情况），
     * 所以必须在动手之前先记下来，否则读不到名字。
     */
    private static ItemStack pendingPlacement = ItemStack.EMPTY;

    private BellHandler() {
    }

    /**
     * 读出物品命名里指定的颜色组。
     * <p>
     * 命名规则：{@code 给事钟} / {@code 給事鐘} / {@code HandyBell}，后面可以再接
     * 一个空格加颜色——颜色可以是内部名（{@code pink}）、原版序号（{@code 6}）、
     * 或者当前语言的译名（{@code 粉红色}）。这样想常用某个配色的人不必放下钟
     * 再拿染料去染，命名一次就固定下来。
     * <p>
     * 空格后面认不出颜色的，就当成普通的名字后缀（比如「给事钟 一号」），
     * 仍然算给事钟，只是颜色用默认值——不能因为多打了两个字就整个失效。
     *
     * @return 颜色 key；不是给事钟返回 null
     */
    public static String namedColor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        Component custom = stack.get(DataComponents.CUSTOM_NAME);
        if (custom == null) {
            return null;
        }
        // 铁砧里也能打出全角空格，先归一，否则切不开
        String name = custom.getString().replace('\u3000', ' ').trim();
        if (name.isEmpty()) {
            return null;
        }

        int cut = name.lastIndexOf(' ');
        String base = cut > 0 ? name.substring(0, cut).trim() : name;
        String suffix = cut > 0 ? name.substring(cut + 1).trim() : "";

        if (!isBaseName(base)) {
            return null;
        }
        String color = ColorRef.parse(suffix);
        return color != null ? color : HandyBellStore.DEFAULT_COLOR;
    }

    private static boolean isBaseName(String s) {
        return s.equals(NAME_ZH) || s.equals(NAME_ZH_TW) || s.equalsIgnoreCase(NAME_EN);
    }

    /** 物品是否被命名成给事钟（可带颜色后缀）。 */
    public static boolean isHandyBellItem(ItemStack stack) {
        return namedColor(stack) != null;
    }

    /** 从染料物品的注册名推断颜色 key，如 minecraft:light_blue_dye → light_blue。 */
    private static String dyeColorOf(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        int colon = id.indexOf(':');
        String path = colon >= 0 ? id.substring(colon + 1) : id;
        return path.endsWith("_dye") ? path.substring(0, path.length() - 4) : null;
    }

    /**
     * 找到这次点击真正指向的钟。
     * 钟的碰撞箱是开口的，从正前方敲时射线会穿过它打到后面的方块，
     * 所以命中的不是钟本身时，再往射线来向找一格。
     */
    private static BlockPos findTargetBell(ClientLevel level, BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        if (level.getBlockState(pos).is(Blocks.BELL)) {
            return pos;
        }
        BlockPos behind = pos.relative(hit.getDirection());
        if (level.getBlockState(behind).is(Blocks.BELL)) {
            return behind;
        }
        return null;
    }

    /**
     * 准星当前指着的那口钟，没有则返回 null。
     * <p>
     * 给 {@code /handybell add} 和添加界面用：在限制放置/拆除的服务器上，
     * 玩家不能放钟，但可以看着已有的钟——这条路径就是为那种场景留的。
     */
    public static BlockPos lookedAtBell() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) {
            return null;
        }
        if (!(mc.hitResult instanceof BlockHitResult hit)) {
            return null;
        }
        return findTargetBell(mc.level, hit);
    }

    /**
     * 拦截右键。返回非 null 表示已经接管，调用方应取消原逻辑。
     */
    public static InteractionResult onUseItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult hit) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return null;
        }

        ItemStack stack = player.getItemInHand(hand);

        // 先记录“可能要放置一口给事钟”，这一刻物品还在手上
        if (isHandyBellItem(stack) && stack.getItem() instanceof BlockItem) {
            pendingPlacement = stack.copy();
        } else {
            pendingPlacement = ItemStack.EMPTY;
        }

        BlockPos pos = findTargetBell(level, hit);
        if (pos == null) {
            return null;
        }

        HandyBellStore store = HandyBellStore.get();
        BellSpot spot = store.spot(level, pos);
        if (spot == null) {
            return null;
        }

        boolean sneaking = player.isShiftKeyDown();

        // 潜行 + 空手：打开配置页
        if (sneaking && stack.isEmpty()) {
            mc.setScreenAndShow(new BellConfigScreen(spot));
            return InteractionResult.SUCCESS;
        }

        // 潜行 + 染料：给钟换色（纯客户端，不消耗染料）
        if (sneaking && stack.is(ItemTags.DYES)) {
            String color = dyeColorOf(stack);
            if (color == null) {
                return null;
            }
            store.setColor(level, pos, color);
            player.sendOverlayMessage(Component.translatable("handybell.message.dyed",
                    Component.translatable("color.minecraft." + color)));
            return InteractionResult.SUCCESS;
        }

        // 普通右键（手上不是方块，避免抢掉放置）
        if (!sneaking && !(stack.getItem() instanceof BlockItem)) {
            ringLocally(level, pos, hit);

            // 直连模式：恰好只有一条命令时，敲响即执行
            if (store.isDirect(spot.color) && store.filledCount(spot.color) == 1) {
                SlotConfig only = store.onlySlot(spot.color);
                if (only != null) {
                    CommandRunner.submit(only);
                    player.sendOverlayMessage(Component.translatable(
                            "handybell.message.executing", only.label()));
                    return InteractionResult.SUCCESS;
                }
            }

            mc.setScreenAndShow(new BellPanelScreen(spot));
            return InteractionResult.SUCCESS;
        }

        return null;
    }

    /** 原版放置流程结束后的善后：如果刚放下的是命名钟，登记坐标。 */
    public static void afterUseItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult hit) {
        ItemStack pending = pendingPlacement;
        pendingPlacement = ItemStack.EMPTY;
        if (pending.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }

        BlockPos pos = findTargetBell(level, hit);
        if (pos == null) {
            return;
        }

        HandyBellStore store = HandyBellStore.get();
        if (store.spot(level, pos) == null) {
            // 命名里指定的颜色在这里生效：放下即属于那个颜色组
            String color = namedColor(pending);
            if (color == null) {
                color = HandyBellStore.DEFAULT_COLOR;
            }
            store.addSpot(level, pos, color);
            player.sendOverlayMessage(Component.translatable("handybell.message.registered",
                    ColorRef.colorName(color)));
            // 只记颜色，不记坐标：坐标等同于玩家位置，不该落进日志
            LOGGER.debug("registered a bell into group {}", color);
        }
    }

    /** 本地播放敲钟：声音 + 摇动，全部只在客户端，不惊动服务端。 */
    private static void ringLocally(ClientLevel level, BlockPos pos, BlockHitResult hit) {
        // 打标：这次 blockEvent 是我们自己敲的，客户端监听那边会跳过，
        // 否则右键敲钟会被当成"红石触发"，命令跑两遍。
        LOCAL_RING = true;
        try {
            level.playLocalSound(
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.BELL_BLOCK, SoundSource.BLOCKS,
                    1.0F, 1.0F, false);
            level.blockEvent(pos, Blocks.BELL, 1, hit.getDirection().get3DDataValue());
        } finally {
            LOCAL_RING = false;
        }
    }
}
