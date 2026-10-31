package local.handybell.ui;

import local.handybell.store.HandyBellStore;
import net.minecraft.network.chat.Component;

/**
 * 颜色 key 到显示名。
 * <p>
 * 直接借用原版的 color.minecraft.* 键——十六个染料色名字与我们的 key 完全一致，
 * 等于免费获得 Minecraft 自带的全部语言翻译，不用自己维护十六种语言的颜色表。
 */
public final class ColorNames {

    private ColorNames() {
    }

    public static Component of(String key) {
        for (String color : HandyBellStore.COLORS) {
            if (color.equals(key)) {
                return Component.translatable("color.minecraft." + key);
            }
        }
        return Component.literal(key);
    }
}
