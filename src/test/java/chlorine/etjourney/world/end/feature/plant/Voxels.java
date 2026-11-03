package chlorine.etjourney.world.end.feature.plant;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

    static int x(long key) {
        return (int) (key >> 42);
    }

    static int y(long key) {
        return (int) (key & 0xFFFFF);
    }

    static int z(long key) {
        return (int) ((key << 22) >> 42);
    }

    /** How many written cells are face-connected to (x, y, z), itself included. */
    int connected(int x, int y, int z) {
        long start = key(x, y, z);
        if (!cells.containsKey(start)) return 0;
        Set<Long> seen = new HashSet<>();
        ArrayDeque<Long> open = new ArrayDeque<>();
        seen.add(start);
        open.add(start);
        int[][] faces = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };
        while (!open.isEmpty()) {
            long c = open.poll();
            for (int[] f : faces) {
                long n = key(x(c) + f[0], y(c) + f[1], z(c) + f[2]);
                if (cells.containsKey(n) && seen.add(n)) open.add(n);
            }
        }
        return seen.size();
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
