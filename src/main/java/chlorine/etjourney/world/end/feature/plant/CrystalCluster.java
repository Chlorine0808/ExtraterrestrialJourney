package chlorine.etjourney.world.end.feature.plant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import chlorine.etjourney.world.end.modifier.EndBlock;

/** The HIGHLAND crystal cluster: leaning crystal pillars with budding crystals inside and buds on the upward faces. */
public final class CrystalCluster implements Grower {

    public static final CrystalCluster INSTANCE = new CrystalCluster();

    private static final int MIN_PILLARS = 3;
    private static final int MAX_PILLARS = 6;
    private static final int MIN_LENGTH = 5;
    private static final int MAX_LENGTH = 12;
    private static final int[][] LEANS = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 }, { 1, 1 }, { 1, -1 }, { -1, 1 },
        { -1, -1 } };
    private static final EndBlock[] BUDS = { EndBlock.CRYSTAL_BUD_SMALL, EndBlock.CRYSTAL_BUD_MEDIUM,
        EndBlock.CRYSTAL_BUD_LARGE, EndBlock.CRYSTAL_CLUSTER };

    private CrystalCluster() {}

    @Override
    public int radius() {
        return 8;
    }

    @Override
    public boolean grow(Random random, int x, int y, int z, int headroom, PlantSink out) {
        int cap = Math.min(MAX_LENGTH, headroom - 1);
        if (cap < MIN_LENGTH) return false;

        // The first pillar is always thick and leans slowly, so some cell is always buried deep enough to bud.
        Set<Long> crystal = new LinkedHashSet<>();
        int pillars = MIN_PILLARS + random.nextInt(MAX_PILLARS - MIN_PILLARS + 1);
        for (int p = 0; p < pillars; p++) {
            int length = MIN_LENGTH + random.nextInt(cap - MIN_LENGTH + 1);
            int slope = p == 0 ? 3 : 2 + random.nextInt(2);
            boolean thick = p == 0 || random.nextBoolean();
            int[] lean = LEANS[random.nextInt(LEANS.length)];
            int bx = random.nextInt(3) - 1;
            int bz = random.nextInt(3) - 1;
            for (int i = 0; i < length; i++) {
                int px = bx + lean[0] * (i / slope);
                int pz = bz + lean[1] * (i / slope);
                for (int dx = 0; dx <= (thick ? 1 : 0); dx++) {
                    for (int dz = 0; dz <= (thick ? 1 : 0); dz++) {
                        crystal.add(key(px + dx, i, pz + dz));
                    }
                }
            }
        }

        List<Long> buried = new ArrayList<>();
        for (long k : crystal) {
            if (neighbours(crystal, k) >= 4) buried.add(k);
        }
        Collections.shuffle(buried, random);
        Set<Long> budding = new LinkedHashSet<>(buried.subList(0, Math.min(buried.size(), 1 + random.nextInt(3))));

        // Buds first, so that a sink which refuses non-air cells still gets whole pillars.
        for (long k : crystal) {
            if (crystal.contains(k + 1) || random.nextInt(100) >= 35) continue;
            put(out, x, y, z, k + 1, BUDS[random.nextInt(BUDS.length)]);
        }
        for (long k : crystal) {
            put(out, x, y, z, k, budding.contains(k) ? EndBlock.BUDDING_CRYSTAL : EndBlock.CRYSTAL);
        }
        return true;
    }

    private static int neighbours(Set<Long> cells, long k) {
        int n = 0;
        for (long step : new long[] { 1, -1, 1L << 8, -(1L << 8), 1L << 16, -(1L << 16) }) {
            if (cells.contains(k + step)) n++;
        }
        return n;
    }

    // Packs offsets as (dx + 64) << 16 | (dz + 64) << 8 | dy, so that a step along one axis is one addition.
    private static long key(int dx, int dy, int dz) {
        return ((long) (dx + 64) << 16) | ((long) (dz + 64) << 8) | dy;
    }

    private static void put(PlantSink out, int x, int y, int z, long k, EndBlock block) {
        out.put(x + (int) (k >> 16) - 64, y + (int) (k & 0xFF), z + (int) ((k >> 8) & 0xFF) - 64, block);
    }
}
