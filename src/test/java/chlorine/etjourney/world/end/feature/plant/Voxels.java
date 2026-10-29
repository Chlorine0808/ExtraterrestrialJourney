package chlorine.etjourney.world.end.feature.plant;

import java.util.HashMap;
import java.util.Map;

import chlorine.etjourney.world.end.modifier.EndBlock;

/** Test sink that records what a plant wrote; a later write to the same cell overwrites the earlier one. */
final class Voxels implements PlantSink {

    final Map<Long, EndBlock> cells = new HashMap<>();
    int minY = Integer.MAX_VALUE;
    int maxY = Integer.MIN_VALUE;

    @Override
    public void put(int x, int y, int z, EndBlock block) {
        cells.put(key(x, y, z), block);
        minY = Math.min(minY, y);
        maxY = Math.max(maxY, y);
    }

    static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFF) << 42) | ((long) (z & 0x3FFFFF) << 20) | (y & 0xFFFFF);
    }

    boolean isEmpty() {
        return cells.isEmpty();
    }

    int count(EndBlock block) {
        int n = 0;
        for (EndBlock b : cells.values()) {
            if (b == block) n++;
        }
        return n;
    }

    /** Largest per-axis (Chebyshev) horizontal distance of any written block from (x, z). */
    int maxDistance(int x, int z) {
        int max = 0;
        for (long k : cells.keySet()) {
            int bx = (int) (k >> 42);
            int bz = (int) ((k << 22) >> 42);
            max = Math.max(max, Math.max(Math.abs(bx - x), Math.abs(bz - z)));
        }
        return max;
    }

    /** Offset of the highest written block above y (empty: -1). */
    int height(int y) {
        return isEmpty() ? -1 : maxY - y;
    }
}
