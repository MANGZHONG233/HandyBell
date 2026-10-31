package local.handybell.compat;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Command Keys 的桥接层。
 * 全部走反射，这样没装 Command Keys 时不会因为类缺失而崩，
 * 也避免和它的版本产生硬绑定。
 */
public final class CommandKeysBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger("handybell");

    private static boolean resolved;
    private static boolean ok;

    private static Method cfgGet;
    private static Method activeProfile;
    private static Method profileGetMacros;
    private static Method macroGetMessages;
    private static Field macroName;
    private static Field msgString;
    private static Field msgDelay;

    private CommandKeysBridge() {
    }

    public static boolean available() {
        return FabricLoader.getInstance().isModLoaded("commandkeys");
    }

    public static boolean usable() {
        resolve();
        return ok;
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        if (!available()) {
            return;
        }
        try {
            Class<?> cfg = Class.forName("dev.terminalmc.commandkeys.config.Config");
            Class<?> profile = Class.forName("dev.terminalmc.commandkeys.config.Profile");
            Class<?> macro = Class.forName("dev.terminalmc.commandkeys.config.Macro");
            Class<?> message = Class.forName("dev.terminalmc.commandkeys.config.Message");

            cfgGet = cfg.getMethod("get");
            activeProfile = cfg.getMethod("activeProfile");
            profileGetMacros = profile.getMethod("getMacros");
            macroGetMessages = macro.getMethod("getMessages");
            macroName = macro.getField("name");
            msgString = message.getField("string");
            msgDelay = message.getField("delayTicks");
            ok = true;
        } catch (Throwable t) {
            ok = false;
            LOGGER.warn("[HandyBell] Command Keys 反射失败: {}", t.toString());
        }
    }

    private static List<?> rawMacros() {
        resolve();
        if (!ok) {
            return List.of();
        }
        try {
            Object config = cfgGet.invoke(null);
            if (config == null) {
                return List.of();
            }
            Object profile = activeProfile.invoke(config);
            if (profile == null) {
                LOGGER.warn("[HandyBell] Command Keys 当前 profile 为 null");
                return List.of();
            }
            Object list = profileGetMacros.invoke(profile);
            if (list instanceof List<?> l) {
                return l;
            }
        } catch (Throwable t) {
            LOGGER.warn("[HandyBell] 读取指令集出错: {}", t.toString());
        }
        return List.of();
    }

    /**
     * 当前 profile 下所有指令集的标识。
     * 名字为空的用 “#索引” 兜底——Command Keys 允许宏不起名，纯靠按键触发。
     */
    public static List<String> macroNames() {
        List<String> out = new ArrayList<>();
        List<?> macros = rawMacros();
        for (int i = 0; i < macros.size(); i++) {
            String name = "";
            try {
                Object n = macroName.get(macros.get(i));
                if (n instanceof String s) {
                    name = s.trim();
                }
            } catch (Throwable ignored) {
            }
            out.add(name.isEmpty() ? ("#" + i) : name);
        }
        LOGGER.debug("读到 {} 个指令集", out.size());
        return out;
    }

    /** 指令集里的第一条非空命令，用来当显示名。 */
    public static String firstCommand(String key) {
        for (Object[] msg : macroMessages(key)) {
            String s = (String) msg[0];
            if (s != null && !s.isBlank()) {
                return s.trim();
            }
        }
        return "";
    }

    /** 给界面显示用：直接展示第一条命令，比“未命名 #3”直观得多。 */
    public static String displayName(String key) {
        String first = firstCommand(key);
        if (first.isEmpty()) {
            if (key.startsWith("#")) {
                return net.minecraft.network.chat.Component.translatable(
                        "handybell.message.empty_macro",
                        Integer.parseInt(key.substring(1)) + 1).getString();
            }
            return key;
        }
        return shorten(first, 30);
    }

    public static String shorten(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    /** 取某个指令集里的命令序列：每项是 {String command, Integer delayTicks}。 */
    public static List<Object[]> macroMessages(String key) {
        List<Object[]> out = new ArrayList<>();
        List<?> macros = rawMacros();
        Object target = null;

        if (key.startsWith("#")) {
            try {
                int idx = Integer.parseInt(key.substring(1));
                if (idx >= 0 && idx < macros.size()) {
                    target = macros.get(idx);
                }
            } catch (NumberFormatException ignored) {
            }
        } else {
            for (Object m : macros) {
                try {
                    if (key.equals(macroName.get(m))) {
                        target = m;
                        break;
                    }
                } catch (Throwable ignored) {
                }
            }
        }

        if (target == null) {
            return out;
        }
        try {
            Object msgs = macroGetMessages.invoke(target);
            if (msgs instanceof List<?> list) {
                for (Object msg : list) {
                    out.add(new Object[]{msgString.get(msg), msgDelay.get(msg)});
                }
            }
        } catch (Throwable t) {
            LOGGER.warn("[HandyBell] 读取指令内容出错: {}", t.toString());
        }
        return out;
    }
}
