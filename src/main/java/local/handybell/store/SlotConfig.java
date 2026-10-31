package local.handybell.store;

import local.handybell.compat.CommandKeysBridge;

/** 一个按钮槽位的配置：要么指向 Command Keys 的指令集，要么是一条手输命令。 */
public class SlotConfig {
    /** Command Keys 的 macro 标识（名字，或 #索引），空表示不用指令集。 */
    public String macro = "";
    /** 手输内容：/、//、!! 开头当命令，其余当聊天。 */
    public String command = "";
    /** 按钮右侧显示的描述。 */
    public String desc = "";

    public SlotConfig() {
    }

    public boolean isEmpty() {
        return macro.isBlank() && command.isBlank();
    }

    /** 按钮上显示什么：优先描述，其次指令集第一条命令，最后是命令本身。 */
    public String label() {
        if (!desc.isBlank()) {
            return desc;
        }
        if (!macro.isBlank() && CommandKeysBridge.available()) {
            String first = CommandKeysBridge.firstCommand(macro);
            if (!first.isBlank()) {
                return first;
            }
            return macro;
        }
        if (!command.isBlank()) {
            return command;
        }
        return "";
    }
}
