package local.handybell.core;

import local.handybell.store.BellSpot;
import local.handybell.store.HandyBellStore;
import local.handybell.ui.AllowRedstoneScreen;
import local.handybell.ui.BellConfigScreen;
import local.handybell.ui.SpotListScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;

import java.util.Arrays;

/**
 * 客户端指令 {@code /handybell ...} 的解析与处理。
 * <p>
 * 这些指令**完全不发给服务端**——由 {@code MixinChatCommand} 在发包前拦下，
 * 所以在任何服务器上都能用，也不会在服务端留下"未知指令"的报错。
 * <p>
 * 两个入口存在的理由不同：
 * <ul>
 *   <li>{@code add} / {@code list} —— 为了让玩家在**不能放置方块**的服务器上
 *       也能登记已有的钟。原来的登记只认"命名后放下"这一条路，
 *       遇到区域保护就直接断了。</li>
 *   <li>{@code allowRedstone} —— 因为它一旦打开，别人就能在你不知情的情况下
 *       远程触发你的命令。放进 UI 会被顺手勾上，放在指令里则需要主动去查。</li>
 * </ul>
 */
public final class HandyBellCommand {

    /** 指令根名，不带斜杠。 */
    private static final String ROOT = "handybell";

    private HandyBellCommand() {
    }

    /**
     * 尝试消费一条命令。
     *
     * @return true 表示这条命令属于本模组、已处理，调用方应当取消发包
     */
    public static boolean handle(String raw) {
        if (raw == null) {
            return false;
        }
        String cmd = raw.trim();
        if (cmd.startsWith("/")) {
            cmd = cmd.substring(1).trim();
        }
        if (!cmd.equalsIgnoreCase(ROOT) && !cmd.toLowerCase().startsWith(ROOT + " ")) {
            return false;
        }

        String[] parts = cmd.split("\\s+");
        if (parts.length < 2) {
            usage();
            return true;
        }

        switch (parts[1].toLowerCase()) {
            case "add" -> add(parts);
            case "config" -> openConfig(parts);
            case "list", "manage" -> openList();
            case "allowredstone" -> allowRedstone(parts);
            default -> usage();
        }
        return true;
    }

    // ------------------------------------------------------------- config

    /**
     * {@code /handybell config <颜色>}
     * <p>
     * 按颜色组直接打开配置页。原来只有"潜行空手右键一口已登记的钟"这一条入口，
     * 意味着想改某个配色的命令，手上得先有一口那个颜色的钟——这没道理，
     * 配置本来就是按颜色组存的，跟具体哪块方块无关。
     */
    private static void openConfig(String[] parts) {
        if (parts.length < 3) {
            usage();
            return;
        }
        String color = ColorRef.parse(parts[2]);
        if (color == null) {
            message(Component.translatable("handybell.cmd.unknown_color", parts[2]));
            return;
        }
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreenAndShow(
                new BellConfigScreen(color)));
    }

    // ---------------------------------------------------------------- add

    /**
     * {@code /handybell add [维度] [x y z] [颜色]}
     * <p>
     * 不给坐标时取准星指着的那口钟——这是最常用的形态，因为玩家本来就站在钟前面。
     * 手输坐标支持 {@code ~} 相对写法，和原版指令的手感一致。
     * <p>
     * 维度放**最前面**、颜色放**末尾**。两个位置都写死，解析就不用标记语法；
     * 要是维度也放末尾，末位一个词既可能是颜色也可能是维度，只能靠猜。
     */
    private static void add(String[] parts) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (mc.level == null || player == null) {
            return;
        }

        String[] rest = Arrays.copyOfRange(parts, 2, parts.length);

        String dimAlias = rest.length > 0 ? Dims.parse(rest[0]) : null;
        if (dimAlias != null) {
            rest = Arrays.copyOfRange(rest, 1, rest.length);
            // 指定了维度就必须给坐标：准星只能指着当前维度的东西，
            // 否则会把当前维度那口钟的坐标登记到别的维度去。
            if (rest.length < 3) {
                usage();
                return;
            }
        }

        String here = mc.level.dimension().identifier().toString();
        String dim = dimAlias != null ? dimAlias : here;
        boolean sameDim = dim.equals(here);

        String color = HandyBellStore.DEFAULT_COLOR;

        boolean colorOnly = rest.length == 1 && ColorRef.parse(rest[0]) != null;
        boolean withColor = rest.length == 4 && ColorRef.parse(rest[3]) != null;

        BlockPos pos;
        if (rest.length == 0 || colorOnly) {
            if (colorOnly) {
                color = ColorRef.parse(rest[0]);
            }
            pos = BellHandler.lookedAtBell();
            if (pos == null) {
                message(Component.translatable("handybell.cmd.add_no_target"));
                return;
            }
        } else if (rest.length == 3 || withColor) {
            if (withColor) {
                color = ColorRef.parse(rest[3]);
            }
            pos = parseCoords(rest, player, sameDim);
            if (pos == null) {
                message(Component.translatable("handybell.cmd.add_bad_coords", String.join(" ", rest)));
                return;
            }
        } else {
            usage();
            return;
        }

        Component where = Dims.where(dim, pos.getX(), pos.getY(), pos.getZ());

        // 只在"同一维度且区块已加载"时才验证那儿真是钟。别的维度查不了，
        // 硬拦反而会让"先登记、以后再放钟"这种做法没法做。
        if (sameDim && mc.level.hasChunkAt(pos) && !mc.level.getBlockState(pos).is(Blocks.BELL)) {
            message(Component.translatable("handybell.cmd.add_not_bell", where));
            return;
        }

        HandyBellStore store = HandyBellStore.get();
        BellSpot existing = store.byKey(BellSpot.key(dim, pos.getX(), pos.getY(), pos.getZ()));
        Component colorName = ColorRef.colorName(color);

        if (existing != null) {
            // 已经登记过就当作改色，比报"已存在"更有用——
            // 换个颜色分组是常见操作，没必要先删再加。
            existing.color = color;
            HandyBellStore.save();
            message(Component.translatable("handybell.cmd.add_updated", where, colorName));
        } else {
            if (sameDim) {
                store.addSpot(mc.level, pos, color);
            } else {
                store.addSpotIn(dim, pos.getX(), pos.getY(), pos.getZ(), color);
            }
            message(Component.translatable("handybell.cmd.added", where, colorName));
        }
    }

    /**
     * 三个整数坐标，支持 {@code ~} / {@code ~1} / {@code ~-2}。
     *
     * @param allowRelative 只有当前维度里 {@code ~} 才有意义——玩家坐标属于当前维度
     * @return 解析失败返回 null
     */
    private static BlockPos parseCoords(String[] tokens, LocalPlayer player, boolean allowRelative) {
        int[] v = new int[3];
        try {
            for (int i = 0; i < 3; i++) {
                String s = tokens[i].trim();
                if (s.startsWith("~")) {
                    if (!allowRelative) {
                        return null;
                    }
                    int base = switch (i) {
                        case 0 -> Mth.floor(player.getX());
                        case 1 -> Mth.floor(player.getY());
                        default -> Mth.floor(player.getZ());
                    };
                    String tail = s.substring(1);
                    v[i] = tail.isEmpty() ? base : base + Integer.parseInt(tail);
                } else if (s.startsWith("^")) {
                    // 局部坐标是按朝向算的，这里没有朝向语义，宁可拒绝也不猜。
                    return null;
                } else {
                    v[i] = Integer.parseInt(s);
                }
            }
        } catch (NumberFormatException e) {
            return null;
        }
        if (v[1] < -512 || v[1] > 512) {
            return null;
        }
        return new BlockPos(v[0], v[1], v[2]);
    }

    // --------------------------------------------------------------- list

    private static void openList() {
        // 排到下一 tick：此刻聊天框还开着，立刻换屏会被它抢回去。
        Minecraft.getInstance().execute(
                () -> Minecraft.getInstance().setScreenAndShow(new SpotListScreen()));
    }

    // ------------------------------------------------------ allowRedstone

    private static void allowRedstone(String[] parts) {
        if (parts.length < 4) {
            usage();
            return;
        }

        String color = ColorRef.parse(parts[2]);
        if (color == null) {
            message(Component.translatable("handybell.cmd.unknown_color", parts[2]));
            return;
        }

        boolean on;
        String value = parts[3].toLowerCase();
        if (value.equals("true") || value.equals("on") || value.equals("1")) {
            on = true;
        } else if (value.equals("false") || value.equals("off") || value.equals("0")) {
            on = false;
        } else {
            usage();
            return;
        }

        if (!on) {
            HandyBellStore.get().setAllowRedstone(color, false);
            HandyBellStore.save();
            message(Component.translatable("handybell.cmd.redstone_off"));
            return;
        }

        // 只有"真正的单人"才允许打开。局域网已开放、或者连的是别人服务器时，
        // 一律拒绝——那些场景下"别人也能触发你的命令"几乎必然变成事故。
        if (!isTrueSingleplayer()) {
            message(Component.translatable("handybell.cmd.not_singleplayer"));
            return;
        }

        Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreenAndShow(
                new AllowRedstoneScreen(color, null)));
    }

    /** 是否处于"真正的单人"：有集成服务器，且没有开放局域网。 */
    public static boolean isTrueSingleplayer() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || !mc.hasSingleplayerServer()) {
            return false;
        }
        var server = mc.getSingleplayerServer();
        return server != null && !server.isPublished();
    }

    /** 在聊天框里回一句话（不发包，纯本地）。 */
    public static void message(Component text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.gui != null) {
            mc.gui.hud.getChat().addClientSystemMessage(text);
        }
    }

    /**
     * 用法分两行：日常的三条，和一条会弹确认页的高级开关。
     * <p>
     * 挤成一行会在聊天框里折成三行，而且把"危险开关"和普通命令摆在一起，
     * 看起来像同一个重量级。
     */
    private static void usage() {
        message(Component.translatable("handybell.cmd.usage"));
        message(Component.translatable("handybell.cmd.usage2"));
    }
}
