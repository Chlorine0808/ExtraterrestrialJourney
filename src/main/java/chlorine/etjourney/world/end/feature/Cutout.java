package chlorine.etjourney.world.end.feature;

import java.util.HashMap;
import java.util.Map;

/** The blocks cut for one biosphere, packed as id << 4 | meta with 0 for air, until every window has them. */
public final class Cutout {

    private final int x0, y0, z0, sx, sy, sz;
    private final int[] blocks;
    /** Tile entity data of the blocks that carry one, by index; kept opaque so this class stays plain Java. */
    private final Map<Integer, Object> tiles = new HashMap<>();
    private int windowsLeft;

    public Cutout(Biosphere b) {
        x0 = (int) Math.floor(b.minX());
        y0 = (int) Math.floor(b.minY());
        z0 = (int) Math.floor(b.minZ());
        sx = (int) Math.ceil(b.maxX()) - x0 + 1;
        sy = (int) Math.ceil(b.maxY()) - y0 + 1;
        sz = (int) Math.ceil(b.maxZ()) - z0 + 1;
        blocks = new int[sx * sy * sz];
        windowsLeft = b.windowCount();
    }

    public boolean covers(int x, int y, int z) {
        return x >= x0 && x < x0 + sx && y >= y0 && y < y0 + sy && z >= z0 && z < z0 + sz;
    }

    public int get(int x, int y, int z) {
        return blocks[index(x, y, z)];
    }

    public void set(int x, int y, int z, int packed) {
        blocks[index(x, y, z)] = packed;
    }

    public void putTile(int x, int y, int z, Object data) {
        tiles.put(index(x, y, z), data);
    }

    /** The tile entity data stored for (x, y, z), or null. */
    public Object tile(int x, int y, int z) {
        return tiles.get(index(x, y, z));
    }

    /** Counts one window copied; true once all of them are. */
    public boolean windowDone() {
        return --windowsLeft <= 0;
    }

    private int index(int x, int y, int z) {
        return ((x - x0) * sz + (z - z0)) * sy + (y - y0);
    }
}
