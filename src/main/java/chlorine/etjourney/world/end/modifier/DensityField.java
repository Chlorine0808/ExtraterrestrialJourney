package chlorine.etjourney.world.end.modifier;

/**
 * Block-level reads of a chunk's 3 x 33 x 3 density grid, interpolated over 8 x 4 x 8-block cells exactly as the
 * vanilla End generator places its blocks. The grid nodes are shared between chunks, so these values are the same
 * whichever chunk asks.
 */
public final class DensityField {

    public static final int SIZE_X = 3, SIZE_Y = 33, SIZE_Z = 3;

    private DensityField() {}

    /** Density at block (lx, ly, lz) of the chunk, with ly counted from the bottom of the field's half. */
    public static double at(double[] f, int lx, int ly, int lz) {
        int i = lx >> 3, j = lz >> 3, k = ly >> 2;
        double tx = (lx & 7) / 8.0, tz = (lz & 7) / 8.0, ty = (ly & 3) / 4.0;
        double a = lerp(f[node(i, j, k)], f[node(i, j, k + 1)], ty);
        double b = lerp(f[node(i, j + 1, k)], f[node(i, j + 1, k + 1)], ty);
        double c = lerp(f[node(i + 1, j, k)], f[node(i + 1, j, k + 1)], ty);
        double d = lerp(f[node(i + 1, j + 1, k)], f[node(i + 1, j + 1, k + 1)], ty);
        double near = a + (c - a) * tx, far = b + (d - b) * tx;
        return near + (far - near) * tz;
    }

    /** How fast the density changes sideways at a block: large on steep faces, near zero on level ground. */
    public static double sideways(double[] f, int lx, int ly, int lz) {
        int i = lx >> 3, j = lz >> 3, k = ly >> 2;
        double tx = (lx & 7) / 8.0, tz = (lz & 7) / 8.0, ty = (ly & 3) / 4.0;
        double a = lerp(f[node(i, j, k)], f[node(i, j, k + 1)], ty);
        double b = lerp(f[node(i, j + 1, k)], f[node(i, j + 1, k + 1)], ty);
        double c = lerp(f[node(i + 1, j, k)], f[node(i + 1, j, k + 1)], ty);
        double d = lerp(f[node(i + 1, j + 1, k)], f[node(i + 1, j + 1, k + 1)], ty);
        double dx = ((c - a) + ((d - b) - (c - a)) * tz) / 8;
        double near = a + (c - a) * tx, far = b + (d - b) * tx;
        double dz = (far - near) / 8;
        return Math.hypot(dx, dz);
    }

    /** The density's change per block at a block, as {x, y, z}. */
    public static double[] gradient(double[] f, int lx, int ly, int lz) {
        int i = lx >> 3, j = lz >> 3, k = ly >> 2;
        double tx = (lx & 7) / 8.0, tz = (lz & 7) / 8.0, ty = (ly & 3) / 4.0;
        double a0 = f[node(i, j, k)], a1 = f[node(i, j, k + 1)], b0 = f[node(i, j + 1, k)],
            b1 = f[node(i, j + 1, k + 1)];
        double c0 = f[node(i + 1, j, k)], c1 = f[node(i + 1, j, k + 1)];
        double d0 = f[node(i + 1, j + 1, k)], d1 = f[node(i + 1, j + 1, k + 1)];
        double a = lerp(a0, a1, ty), b = lerp(b0, b1, ty), c = lerp(c0, c1, ty), d = lerp(d0, d1, ty);
        double dx = ((c - a) + ((d - b) - (c - a)) * tz) / 8;
        double near = a + (c - a) * tx, far = b + (d - b) * tx;
        double dz = (far - near) / 8;
        double ua = a1 - a0, ub = b1 - b0, uc = c1 - c0, ud = d1 - d0;
        double unear = ua + (uc - ua) * tx, ufar = ub + (ud - ub) * tx;
        double dy = (unear + (ufar - unear) * tz) / 4;
        return new double[] { dx, dy, dz };
    }

    /** The largest sideways change per block along the edges of cell (i, j, k). */
    public static double cellSlope(double[] f, int i, int j, int k) {
        double most = 0;
        for (int l = k; l <= k + 1; l++) {
            double a = f[node(i, j, l)], b = f[node(i, j + 1, l)], c = f[node(i + 1, j, l)],
                d = f[node(i + 1, j + 1, l)];
            most = Math.max(
                most,
                Math.max(Math.max(Math.abs(c - a), Math.abs(d - b)), Math.max(Math.abs(b - a), Math.abs(d - c))));
        }
        return most / 8 * Math.sqrt(2);
    }

    /** Whether any value inside cell (i, j, k) can lie within margin of zero. */
    public static boolean cellCrosses(double[] f, int i, int j, int k, double margin) {
        double low = Double.MAX_VALUE, high = -Double.MAX_VALUE;
        for (int di = 0; di <= 1; di++) {
            for (int dj = 0; dj <= 1; dj++) {
                for (int dk = 0; dk <= 1; dk++) {
                    double v = f[node(i + di, j + dj, k + dk)];
                    low = Math.min(low, v);
                    high = Math.max(high, v);
                }
            }
        }
        return low <= margin && high >= -margin;
    }

    private static int node(int i, int j, int k) {
        return (i * SIZE_Z + j) * SIZE_Y + k;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
