package chlorine.etjourney.world.end.feature.plant;

import java.util.ArrayDeque;
import java.util.Random;

import chlorine.etjourney.world.end.feature.sdf.Sdf;
import chlorine.etjourney.world.end.modifier.EndBlock;

/** The Lucentwoods bush, after BetterEnd's lucernia bush: a squashed, lumpy ball of leaves on one log. */
public final class LucentBush implements Grower {

    public static final LucentBush INSTANCE = new LucentBush();

    /** Leaves at this many face steps from the log or more are dropped, as upstream does. */
    private static final int REACH = 7;
    private static final int[][] FUR_FACES = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };
    private static final int[][] FACES = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 },
        { 0, 0, -1 } };

    private LucentBush() {}

    @Override
    public int radius() {
        return REACH;
    }

    @Override
    public boolean grow(Random random, int x, int y, int z, int headroom, PlantSink out) {
        if (headroom < 3) return false;
        double radius = 1.8 + random.nextDouble() * 1.7;
        long seed = random.nextLong();
        Sdf ball = Sdf.sphere(radius)
            .scale(1, 0.5, 1)
            .displace(LucentShapes.noise(seed, 3))
            .displace(LucentShapes.jitter(seed ^ 0x5DEECE66DL, 2));
        Sdf bush = ball.subtract(ball.translate(0, -radius, 0));

        boolean[][][] leaf = new boolean[2 * REACH + 1][REACH + 1][2 * REACH + 1];
        for (int dy = 0; dy <= Math.min(REACH, headroom - 1); dy++)
            for (int dx = -REACH; dx <= REACH; dx++) for (int dz = -REACH; dz <= REACH; dz++)
                if (Math.abs(dx) + dy + Math.abs(dz) < REACH && bush.distance(dx, dy, dz) < 0)
                    leaf[dx + REACH][dy][dz + REACH] = true;
        // The log's neighbours are leaves whatever the shape says, so the bush always sits on its log.
        leaf[REACH][0][REACH] = false;
        leaf[REACH][1][REACH] = true;
        for (int[] f : FACES) if (f[1] == 0) leaf[REACH + f[0]][0][REACH + f[2]] = true;
        keepConnected(leaf);

        out.put(x, y, z, EndBlock.LUCENT_LOG);
        for (int dx = -REACH; dx <= REACH; dx++)
            for (int dy = 0; dy <= REACH; dy++) for (int dz = -REACH; dz <= REACH; dz++)
                if (leaf[dx + REACH][dy][dz + REACH]) out.put(x + dx, y + dy, z + dz, EndBlock.LUCENT_LEAVES);
        for (int dx = -REACH; dx <= REACH; dx++)
            for (int dy = 0; dy <= REACH; dy++) for (int dz = -REACH; dz <= REACH; dz++) {
                if (!leaf[dx + REACH][dy][dz + REACH]) continue;
                for (int[] f : FUR_FACES) {
                    int fx = dx + f[0], fy = dy + f[1], fz = dz + f[2];
                    if (Math.abs(fx) > REACH || Math.abs(fz) > REACH || fy >= headroom) continue;
                    if (fy <= REACH && leaf[fx + REACH][fy][fz + REACH]) continue;
                    if (fx == 0 && fy == 0 && fz == 0) continue;
                    // Fur never hangs under a leaf; that side is left bare.
                    if (fy + 1 <= REACH && leaf[fx + REACH][fy + 1][fz + REACH]) continue;
                    out.put(x + fx, y + fy, z + fz, EndBlock.LUCENT_FUR);
                }
            }
        return true;
    }

    /**
     * Drops the leaves not face-connected to the log, as upstream fills only what its start cell reaches, and those
     * farther from it through leaves than they may stand without decaying.
     */
    private static void keepConnected(boolean[][][] leaf) {
        boolean[][][] seen = new boolean[leaf.length][leaf[0].length][leaf[0][0].length];
        ArrayDeque<int[]> open = new ArrayDeque<>();
        open.add(new int[] { REACH, 0, REACH, 0 });
        while (!open.isEmpty()) {
            int[] c = open.poll();
            for (int[] f : FACES) {
                int x = c[0] + f[0], y = c[1] + f[1], z = c[2] + f[2];
                if (x < 0 || y < 0 || z < 0 || x >= leaf.length || y >= leaf[0].length || z >= leaf[0][0].length)
                    continue;
                if (!leaf[x][y][z] || seen[x][y][z] || c[3] >= LEAF_REACH) continue;
                seen[x][y][z] = true;
                open.add(new int[] { x, y, z, c[3] + 1 });
            }
        }
        for (int x = 0; x < leaf.length; x++) for (int y = 0; y < leaf[0].length; y++)
            for (int z = 0; z < leaf[0][0].length; z++) leaf[x][y][z] &= seen[x][y][z];
    }
}
