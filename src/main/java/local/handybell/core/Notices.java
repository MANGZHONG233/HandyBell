package local.handybell.core;

import local.handybell.store.BellSpot;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 给玩家的短提示（快捷栏上方那行 actionbar）。
 * <p>
 * 只在具体动作发生的瞬间调用一次，不挂计时器、不留状态。
 */
public final class Notices {

    /** 彩蛋概率：百分比。发布版 1%。 */
    private static final int EXILE_CHANCE_PERCENT = 1;

    private Notices() {
    }

    /** 某口给事钟被取消登记之后。 */
    public static void bellUnregistered(BellSpot spot) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gui == null || mc.gui.hud == null || spot == null) {
            return;
        }

        Component message;
        boolean chinese = isChinese();
        if ((chinese || isEnglish())
                && ThreadLocalRandom.current().nextInt(100) < EXILE_CHANCE_PERCENT) {
            // 彩蛋只在中英环境出现，其余语言安静走正常文案。
            // 中文用"大不敬"而非"大不恭"——后者是乌台诗案里舒亶弹劾苏轼的罪名，
            // 带文字狱色彩，不适合当梗。
            message = Component.literal(chinese ? chineseEgg(spot) : englishEgg(spot));
        } else {
            message = Component.translatable("handybell.message.unregistered",
                    Component.translatable("handybell.message.at",
                            dimensionName(spot.dim), spot.x, spot.y, spot.z));
        }

        mc.gui.hud.setOverlayMessage(message, false);
    }

    /** 当前语言是否为中文字形（zh_cn / zh_hk / zh_tw 等）。 */
    private static boolean isChinese() {
        String code = selectedLanguage();
        return code != null && code.startsWith("zh");
    }

    /** 英文环境（含 en_gb 等变体）。 */
    private static boolean isEnglish() {
        String code = selectedLanguage();
        return code != null && code.startsWith("en");
    }

    private static String selectedLanguage() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getLanguageManager() == null) {
            return null;
        }
        return mc.getLanguageManager().getSelected();
    }

    private static Component dimensionName(String dim) {
        return Dims.name(dim);
    }

    /** 中文彩蛋：维度名写死简体，避免中英混排。 */
    private static String chineseEgg(BellSpot spot) {
        String dim = switch (spot.dim) {
            case "minecraft:overworld" -> "主世界";
            case "minecraft:the_nether" -> "下界";
            case "minecraft:the_end" -> "末地";
            default -> spot.dim;
        };
        return dim + " (" + spot.x + ", " + spot.y + ", " + spot.z + ")"
                + " 的给事钟因大不敬发配辽东充军去了";
    }

    /**
     * 英文彩蛋。
     * <p>
     * 整句做成一个"投递地址"：handybell@OW x y z。@ 在英文里本就读 at，
     * 所以 handle@dimension 的读法和口语完全一致，只是换了个更紧的写法——
     * 而且句子内容说"它被发配了"，格式却在给它标注地址，形式和内容对上。
     * <p>
     * naughty-cornered 取自英式育儿语 naughty corner / naughty step（罚站墙角），
     * 把"发配"降格成小孩待遇；un-bell-ievable 是 unbelievable 里正好嵌着 bell；
     * hand-foolery 既接 foolery（胡闹），又回扣 HandyBell 的 hand。
     */
    private static String englishEgg(BellSpot spot) {
        return String.format("handybell@%s %d %d %d: naughty-cornered"
                        + " for un-bell-ievable hand-foolery.",
                shortDimension(spot.dim), spot.x, spot.y, spot.z);
    }

    /**
     * 彩蛋里用的短维度名。
     * <p>
     * 只有 Overworld 值得缩——它最长。Nether 和 End 本身够短，
     * 缩成 N / E 反而会和方向混淆，且省不下多少宽度。
     */
    private static String shortDimension(String dim) {
        return switch (dim) {
            case "minecraft:overworld" -> "OW";
            case "minecraft:the_nether" -> "Nether";
            case "minecraft:the_end" -> "End";
            default -> dim;
        };
    }
}
