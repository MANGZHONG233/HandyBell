package local.handybell.core;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 维度名的本地化显示、别名解析与坐标显示，几处提示共用。
 * <p>
 * 放在这里而不是各自复制一份，是因为 {@code /handybell add}、管理页、
 * 取消登记的提示都要显示同一个维度名，措辞一旦分叉就对不齐了。
 */
public final class Dims {

    /** 三个原版维度，顺序就是界面上按钮的顺序。 */
    public static final String[] VANILLA = {
            "minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"
    };

    private Dims() {
    }

    /** 维度 id → 本地化名称；原版三个有译名，其余原样显示 id。 */
    public static Component name(String dim) {
        String key = switch (dim) {
            case "minecraft:overworld" -> "handybell.dimension.overworld";
            case "minecraft:the_nether" -> "handybell.dimension.the_nether";
            case "minecraft:the_end" -> "handybell.dimension.the_end";
            default -> null;
        };
        return key == null ? Component.literal(dim) : Component.translatable(key);
    }

    /**
     * 解析指令里的维度写法。
     * <p>
     * 两种都能写：原版三个的短别名（{@code ow / nether / end}，也接受全称），
     * 以及任何带命名空间的完整 id（{@code mypack:mining}）——模组或服务器
     * 自定义的维度只能靠后者，而且没法预先列举，所以带冒号的一律原样收下。
     * <p>
     * 别名刻意避开了十六个染料色名，这样 {@code add nether} 不会被当成
     * "给瞄准的钟换个颜色"。
     *
     * @return 规范的维度 id，认不出返回 null
     */
    public static String parse(String token) {
        if (token == null) {
            return null;
        }
        String t = token.trim().toLowerCase();
        if (t.isEmpty()) {
            return null;
        }
        int colon = t.indexOf(':');
        if (colon > 0) {
            return t;
        }
        return switch (t) {
            case "ow", "overworld" -> "minecraft:overworld";
            case "nether", "the_nether" -> "minecraft:the_nether";
            case "end", "the_end" -> "minecraft:the_end";
            default -> null;
        };
    }

    /**
     * 界面上要展示的维度按钮：原版三个，再加上玩家当前所在的维度（如果不是那三个之一）。
     * <p>
     * 自定义维度不预先列举——列不完，也没必要；玩家要登记别的维度时，
     * 要么站到那个维度里，要么用指令直接写 id。
     */
    public static List<String> choices(String current) {
        List<String> out = new ArrayList<>(List.of(VANILLA));
        if (current != null && !out.contains(current)) {
            out.add(current);
        }
        return out;
    }

    /**
     * "主世界（249, 104, 23）"。
     * <p>
     * 坐标套括号是为了和维度名分开：自定义维度形如 {@code mypack:mining}，
     * 直接跟三个数字连排会读成一体。括号本身交给语言文件决定，
     * 中文用全角、其他语言用半角。
     */
    public static Component where(String dim, int x, int y, int z) {
        return Component.translatable("handybell.message.at", name(dim), x, y, z);
    }
}
