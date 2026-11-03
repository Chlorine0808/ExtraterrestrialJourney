package chlorine.etjourney.world.end.feature.sdf;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Set;

/** Turns a shape into cells: a face-connected flood from a start cell through the inside, as BCLib's fillRecursive. */
public final class SdfFill {

    private static final int BITS = 21;
    private static final long MASK = (1L << BITS) - 1;
    private static final int BIAS = 1 << (BITS - 1);
    private static final int[][] FACES = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 },
        { 0, 0, -1 } };

    private SdfFill() {}

    /**
     * Cells (packed with {@link #key}) inside {@code shape} and reachable from (sx, sy, sz) through inside cells;
     * {@code bounds} is {minX, minY, minZ, maxX, maxY, maxZ}, inclusive.
     */
    public static Set<Long> fill(Sdf shape, int sx, int sy, int sz, int[] bounds) {
        Set<Long> cells = new LinkedHashSet<>();
        if (!inside(shape, sx, sy, sz, bounds)) return cells;
        ArrayDeque<Long> open = new ArrayDeque<>();
        long start = key(sx, sy, sz);
        cells.add(start);
        open.add(start);
        while (!open.isEmpty()) {
            long c = open.poll();
            int x = x(c), y = y(c), z = z(c);
            for (int[] f : FACES) {
                int nx = x + f[0], ny = y + f[1], nz = z + f[2];
                long n = key(nx, ny, nz);
                if (cells.contains(n) || !inside(shape, nx, ny, nz, bounds)) continue;
                cells.add(n);
                open.add(n);
            }
        }
        return cells;
    }

    private static boolean inside(Sdf shape, int x, int y, int z, int[] b) {
        return x >= b[0] && y >= b[1]
            && z >= b[2]
            && x <= b[3]
            && y <= b[4]
            && z <= b[5]
            && shape.distance(x, y, z) < 0;
    }

    public static long key(int x, int y, int z) {
        return ((long) (x + BIAS) & MASK) << (2 * BITS) | ((long) (y + BIAS) & MASK) << BITS
            | ((long) (z + BIAS) & MASK);
    }

    public static int x(long key) {
        return (int) ((key >>> (2 * BITS)) & MASK) - BIAS;
    }

    public static int y(long key) {
        return (int) ((key >>> BITS) & MASK) - BIAS;
    }

    public static int z(long key) {
        return (int) (key & MASK) - BIAS;
    }

    public static long offset(long key, int dx, int dy, int dz) {
        return key(x(key) + dx, y(key) + dy, z(key) + dz);
    }
}
