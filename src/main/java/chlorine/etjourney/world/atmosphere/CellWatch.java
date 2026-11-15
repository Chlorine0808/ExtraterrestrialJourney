package chlorine.etjourney.world.atmosphere;

import java.util.HashMap;
import java.util.Map;

/** Remembers each player's last dimension and grid cell, to tell when a fresh grid is due. */
final class CellWatch {

    private final Map<Object, long[]> last = new HashMap<>();

    /** True on first sight, or when the dimension or the cell differs from last time. */
    boolean moved(Object player, int dimension, int cellX, int cellZ) {
        long[] seen = last.get(player);
        if (seen != null && seen[0] == dimension && seen[1] == cellX && seen[2] == cellZ) return false;
        last.put(player, new long[] { dimension, cellX, cellZ });
        return true;
    }

    void forget(Object player) {
        last.remove(player);
    }
}
