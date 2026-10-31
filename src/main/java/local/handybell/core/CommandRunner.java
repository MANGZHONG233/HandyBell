package local.handybell.core;

import local.handybell.compat.CommandKeysBridge;
import local.handybell.store.SlotConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

import java.util.ArrayDeque;
import java.util.Deque;

/** 命令执行队列：按 Command Keys 的 delayTicks 逐条发送。 */
public final class CommandRunner {

    private record Pending(String command, int delay) {
        Pending tickDown() {
            return new Pending(command, delay - 1);
        }
    }

    private static final Deque<Pending> QUEUE = new ArrayDeque<>();

    private CommandRunner() {
    }

    public static void submit(SlotConfig slot) {
        if (slot == null) {
            return;
        }
        if (!slot.macro.isBlank() && CommandKeysBridge.available()) {
            for (Object[] msg : CommandKeysBridge.macroMessages(slot.macro)) {
                String cmd = (String) msg[0];
                int delay = msg[1] instanceof Integer i ? i : 0;
                if (cmd != null && !cmd.isBlank()) {
                    QUEUE.addLast(new Pending(cmd, Math.max(delay, 0)));
                }
            }
            return;
        }
        if (!slot.command.isBlank()) {
            QUEUE.addLast(new Pending(slot.command, 0));
        }
    }

    public static void tick() {
        if (QUEUE.isEmpty()) {
            return;
        }
        Pending head = QUEUE.peekFirst();
        if (head.delay() > 0) {
            QUEUE.pollFirst();
            QUEUE.addFirst(head.tickDown());
            return;
        }
        QUEUE.pollFirst();
        send(head.command());
    }

    /**
     * 三种走法：
     * 1. //  或 !! 开头 → 原样当命令发（WorldEdit / 插件类命令，前缀本身就是命令的一部分）
     * 2. / 开头     → 去掉斜杠后当命令发
     * 3. 其它       → 当聊天内容发出去
     */
    private static void send(String raw) {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener conn = mc.getConnection();
        if (conn == null) {
            return;
        }
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) {
            return;
        }
        if (text.startsWith("//") || text.startsWith("!!")) {
            conn.sendCommand(text);
        } else if (text.startsWith("/")) {
            conn.sendCommand(text.substring(1));
        } else {
            conn.sendChat(text);
        }
    }
}
