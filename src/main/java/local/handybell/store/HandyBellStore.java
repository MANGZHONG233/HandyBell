package local.handybell.store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 本地存档：哪些坐标是给事钟，以及 16 种颜色各自的 9 个槽位配置。 */
public final class HandyBellStore {

    public static final String[] COLORS = {
            "white", "orange", "magenta", "light_blue",
            "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue",
            "brown", "green", "red", "black"
    };
    public static final String DEFAULT_COLOR = "yellow";
    public static final int SLOT_COUNT = 9;

    /** 颜色名在 COLORS 里的下标，找不到返回 -1。 */
    public static int indexOf(String color) {
        for (int i = 0; i < COLORS.length; i++) {
            if (COLORS[i].equals(color)) {
                return i;
            }
        }
        return -1;
    }

    /** 是否是十六种染料色之一。 */
    public static boolean isValidColor(String color) {
        return indexOf(color) >= 0;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static HandyBellStore instance;

    public Map<String, BellSpot> spots = new LinkedHashMap<>();
    public Map<String, List<SlotConfig>> groups = new LinkedHashMap<>();
    /** 颜色 → 敲响时是否直接执行唯一的那条命令（不弹面板）。 */
    public Map<String, Boolean> direct = new LinkedHashMap<>();
    /**
     * 颜色 → 是否允许"红石信号 / 投掷物"敲响时也执行命令。
     * <p>
     * 默认关闭：钟的响是公共事件（一个人拉杆，所有人都收到），
     * 若默认开启，公共据点里任何人随手一拉就会把所有人绑定的命令一起跑掉。
     * 需要"让别人替我敲"（比如自救）时，把那个颜色组单独打开。
     */
    public Map<String, Boolean> redstone = new LinkedHashMap<>();

    public static HandyBellStore get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("handybell.json");
    }

    private static HandyBellStore load() {
        HandyBellStore store = new HandyBellStore();
        Path path = file();
        if (Files.exists(path)) {
            try {
                String json = Files.readString(path, StandardCharsets.UTF_8);
                HandyBellStore loaded = GSON.fromJson(json, HandyBellStore.class);
                if (loaded != null) {
                    store = loaded;
                }
            } catch (Exception ignored) {
            }
        }
        store.normalize();
        return store;
    }

    private void normalize() {
        if (spots == null) {
            spots = new LinkedHashMap<>();
        }
        if (groups == null) {
            groups = new LinkedHashMap<>();
        }
        if (direct == null) {
            direct = new LinkedHashMap<>();
        }
        if (redstone == null) {
            redstone = new LinkedHashMap<>();
        }
        for (String color : COLORS) {
            List<SlotConfig> list = groups.computeIfAbsent(color, k -> new ArrayList<>());
            while (list.size() < SLOT_COUNT) {
                list.add(new SlotConfig());
            }
            while (list.size() > SLOT_COUNT) {
                list.remove(list.size() - 1);
            }
            direct.putIfAbsent(color, Boolean.FALSE);
            redstone.putIfAbsent(color, Boolean.FALSE);
        }
    }

    public static void save() {
        if (instance == null) {
            return;
        }
        try {
            Path path = file();
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(instance), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    public List<SlotConfig> group(String color) {
        normalize();
        List<SlotConfig> list = groups.get(color);
        return list != null ? list : groups.get(DEFAULT_COLOR);
    }

    public boolean isDirect(String color) {
        normalize();
        return Boolean.TRUE.equals(direct.get(color));
    }

    public void setDirect(String color, boolean value) {
        normalize();
        direct.put(color, value);
    }

    /** 该颜色组是否允许红石/投掷物触发命令。默认 false。 */
    public boolean allowRedstone(String color) {
        normalize();
        return Boolean.TRUE.equals(redstone.get(color));
    }

    public void setAllowRedstone(String color, boolean value) {
        normalize();
        redstone.put(color, value);
    }

    /** 该颜色组里有内容的槽位数量。 */
    public int filledCount(String color) {
        int n = 0;
        for (SlotConfig cfg : group(color)) {
            if (!cfg.isEmpty()) {
                n++;
            }
        }
        return n;
    }

    public SlotConfig onlySlot(String color) {
        for (SlotConfig cfg : group(color)) {
            if (!cfg.isEmpty()) {
                return cfg;
            }
        }
        return null;
    }

    /** 该坐标是否是给事钟；顺带校验那里确实还是钟（被拆掉就当作不是）。 */
    public BellSpot spot(Level level, BlockPos pos) {
        String dim = level.dimension().identifier().toString();
        BellSpot spot = spots.get(BellSpot.key(dim, pos.getX(), pos.getY(), pos.getZ()));
        if (spot == null) {
            return null;
        }
        if (!level.getBlockState(pos).is(Blocks.BELL)) {
            return null;
        }
        return spot;
    }

    public BellSpot addSpot(Level level, BlockPos pos, String color) {
        String dim = level.dimension().identifier().toString();
        return addSpotIn(dim, pos.getX(), pos.getY(), pos.getZ(), color);
    }

    /**
     * 按维度 id 直接登记，不经过 {@link Level}。
     * <p>
     * 给"给别的维度登记坐标"用——那种情况下手上根本没有那个维度的 Level 对象，
     * 也不能因为区块没加载就放弃。
     */
    public BellSpot addSpotIn(String dim, int x, int y, int z, String color) {
        BellSpot spot = new BellSpot(dim, x, y, z, color);
        spots.put(spot.key(), spot);
        save();
        return spot;
    }

    public void setColor(Level level, BlockPos pos, String color) {
        BellSpot spot = spot(level, pos);
        if (spot == null) {
            spot = addSpot(level, pos, color);
        }
        spot.color = color;
        save();
    }

    public void remove(Level level, BlockPos pos) {
        String dim = level.dimension().identifier().toString();
        if (spots.remove(BellSpot.key(dim, pos.getX(), pos.getY(), pos.getZ())) != null) {
            save();
        }
    }

    /** 全部登记，按添加顺序。返回副本，调用方拿去遍历不会踩到并发修改。 */
    public List<BellSpot> allSpots() {
        normalize();
        return new ArrayList<>(spots.values());
    }

    public BellSpot byKey(String key) {
        normalize();
        return spots.get(key);
    }

    /**
     * 只改颜色，不碰坐标。
     * <p>
     * 管理页和 {@code /handybell add} 都走这里——它们拿不到 Level 对象
     * （可能跨维度、也可能区块根本没加载），所以不能复用按坐标查找的那套。
     */
    public boolean setColorByKey(String key, String color) {
        normalize();
        BellSpot spot = spots.get(key);
        if (spot == null) {
            return false;
        }
        spot.color = color;
        save();
        return true;
    }

    public boolean removeByKey(String key) {
        normalize();
        if (spots.remove(key) == null) {
            return false;
        }
        save();
        return true;
    }

    /** 某个维度里登记了多少口钟。管理页上做统计用。 */
    public int countIn(String dim) {
        normalize();
        int n = 0;
        for (BellSpot spot : spots.values()) {
            if (dim.equals(spot.dim)) {
                n++;
            }
        }
        return n;
    }

    /**
     * 清掉当前维度里“方块已经不是钟”的登记。
     * 只处理已经加载的区块，没加载的一律跳过——否则会把远处没同步的钟误删。
     */
    public int prune(Level level) {
        String dim = level.dimension().identifier().toString();
        int removed = 0;
        var it = spots.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, BellSpot> entry = it.next();
            BellSpot spot = entry.getValue();
            if (!dim.equals(spot.dim)) {
                continue;
            }
            BlockPos pos = new BlockPos(spot.x, spot.y, spot.z);
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            if (!level.getBlockState(pos).is(Blocks.BELL)) {
                it.remove();
                removed++;
            }
        }
        if (removed > 0) {
            save();
        }
        return removed;
    }
}
