package local.handybell.core;

import local.handybell.store.HandyBellStore;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;

/**
 * 把"颜色"的各种写法归一到内部 key。
 * <p>
 * 认三种写法，因为它们各自出现在不同场合：
 * <ul>
 *   <li><b>内部名</b> {@code pink} —— 写配置、抄文档、跨语言交流时用</li>
 *   <li><b>原版序号</b> {@code 6} —— 铁砧命名里打一个字符比打五个字母省事，
 *       而且十六种颜色在铁砧里能打出的名字长度是有上限的</li>
 *   <li><b>当前语言的译名</b> {@code 粉红色} / {@code Pink} —— 玩家眼里最直观的那个</li>
 * </ul>
 * 序号用的是 {@code DyeColor} 的顺序，和 {@link HandyBellStore#COLORS} 完全一致：
 * 0 白、1 橙、2 品红、3 淡蓝、4 黄、5 黄绿、6 粉红、7 灰、8 淡灰、
 * 9 青、10 紫、11 蓝、12 棕、13 绿、14 红、15 黑。
 * <p>
 * 译名不建表，每次现查十六个键——这样跟着游戏语言走，不用维护十三个反向词典。
 */
public final class ColorRef {

    private ColorRef() {
    }

    /**
     * @return 内部 key，认不出返回 null
     */
    public static String parse(String token) {
        if (token == null) {
            return null;
        }
        String t = token.trim();
        if (t.isEmpty()) {
            return null;
        }
        String lower = t.toLowerCase();

        if (HandyBellStore.isValidColor(lower)) {
            return lower;
        }

        if (isDigits(lower)) {
            return byId(Integer.parseInt(lower));
        }

        // 译名比较要忽略大小写：德语名词首字母大写，法语/西语可能带重音，
        // 玩家不一定照着语言文件一字不差地打。
        for (String key : HandyBellStore.COLORS) {
            if (colorName(key).getString().equalsIgnoreCase(t)) {
                return key;
            }
        }
        return null;
    }

    /** 某颜色的当前语言译名。 */
    public static Component colorName(String key) {
        return Component.translatable("color.minecraft." + key);
    }

    /**
     * 序号对应的内部 key，越界返回 null。
     * <p>
     * 走 {@code DyeColor} 而不是直接下标我们的数组，是为了让"序号"这个词
     * 名副其实——它指的是原版染料序，不是本模组碰巧排出来的顺序。
     * {@code byId} 对越界会抛，所以先自己拦一道。
     */
    public static String byId(int id) {
        if (id < 0 || id >= 16) {
            return null;
        }
        DyeColor dye = DyeColor.byId(id);
        if (dye == null) {
            return null;
        }
        String name = dye.getSerializedName();
        return HandyBellStore.isValidColor(name) ? name : null;
    }

    private static boolean isDigits(String s) {
        if (s.isEmpty() || s.length() > 2) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
