package local.handybell.store;

/** 被登记的一个“给事钟”：世界坐标 + 所属颜色分组。 */
public class BellSpot {
    public int x;
    public int y;
    public int z;
    public String dim = "minecraft:overworld";
    public String color = "yellow";

    public BellSpot() {
    }

    public BellSpot(String dim, int x, int y, int z, String color) {
        this.dim = dim;
        this.x = x;
        this.y = y;
        this.z = z;
        this.color = color;
    }

    public String key() {
        return key(dim, x, y, z);
    }

    public static String key(String dim, int x, int y, int z) {
        return dim + '|' + x + '|' + y + '|' + z;
    }
}
