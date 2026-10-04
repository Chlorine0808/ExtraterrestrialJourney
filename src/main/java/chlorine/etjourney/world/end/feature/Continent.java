package chlorine.etjourney.world.end.feature;

import java.util.ArrayList;
import java.util.List;

import chlorine.etjourney.world.end.noise.Fractal;
import chlorine.etjourney.world.end.noise.Hash;
import chlorine.etjourney.world.end.noise.ValueNoise;
import chlorine.etjourney.world.end.noise.Warp;

/**
 * End-stone continents in the spirit of post-1.9 outer Ends, written from the idea, not the code. Seeds sit on a
 * 16-block grid where a clustering noise dips low, so neighbouring seeds fuse into continents and lone seeds stay
 * small islands.
 */
public final class Continent {

    public static final double MAX_HEIGHT = 80;
    private static final int SEED_CELL = 16;
    /** Cells searched beyond the range: a seed still raises the height above -100 up to 200 / 14 * 8 blocks away. */
    private static final int SEARCH_CELLS = 8;
    private static final double SEED_RING = 1024;
    private static final double SEED_THRESHOLD = 0.23;
    private static final double MIN_FALLOFF = 14, MAX_FALLOFF = 24;
    private static final double WARP = 28, WARP_SCALE = 110;
    private static final double COAST = 30;
    private static final long SALT = 0x510E527FADE682D1L;
    private static final long WARP_SALT = 0x5BE0CD19137E2179L;

    private Continent() {}

    /** One seed: centre (block) and height falloff per 8 blocks. */
    public static final class Seed {

        public final double x, z, falloff;

        Seed(double x, double z, double falloff) {
            this.x = x;
            this.z = z;
            this.falloff = falloff;
        }
    }

    public static List<Seed> seedsNear(long seed, double x, double z, double range) {
        List<Seed> out = new ArrayList<>();
        long s = seed ^ SALT;
        int c0x = (int) Math.floor((x - range) / SEED_CELL) - SEARCH_CELLS;
        int c1x = (int) Math.floor((x + range) / SEED_CELL) + SEARCH_CELLS;
        int c0z = (int) Math.floor((z - range) / SEED_CELL) - SEARCH_CELLS;
        int c1z = (int) Math.floor((z + range) / SEED_CELL) + SEARCH_CELLS;
        for (int cx = c0x; cx <= c1x; cx++) {
            for (int cz = c0z; cz <= c1z; cz++) {
                double sx = cx * SEED_CELL + 8, sz = cz * SEED_CELL + 8;
                if (sx * sx + sz * sz <= SEED_RING * SEED_RING) continue;
                double cluster = 0.7 * ValueNoise.mask(s + 1, sx, sz, 160) + 0.3 * ValueNoise.mask(s + 2, sx, sz, 48);
                if (cluster > SEED_THRESHOLD + 0.25 * Hash.hash01(s + 3, cx, cz) - 0.125) continue;
                double falloff = MIN_FALLOFF + Hash.hash01(s + 4, cx, cz) * (MAX_FALLOFF - MIN_FALLOFF);
                out.add(new Seed(sx, sz, falloff));
            }
        }
        return out;
    }

    /** Height from the seeds alone: 0 at a rim, up to MAX_HEIGHT inside, negative over the void. */
    public static double rawHeight(List<Seed> seeds, double x, double z) {
        double best = -100;
        for (Seed s : seeds) {
            best = Math.max(best, 100 - Math.hypot(x - s.x, z - s.z) / 8 * s.falloff);
        }
        return Math.min(MAX_HEIGHT, best);
    }

    /** The frame continent outlines and mountain footprints are measured in. */
    public static double[] warped(long seed, double x, double z) {
        return Warp.warp(seed ^ WARP_SALT, x, z, WARP, WARP_SCALE);
    }

    /** Fine jitter added to land height, so coastlines are ragged. */
    public static double coast(long seed, double x, double z) {
        return (Fractal.fbm(seed ^ (WARP_SALT + 9), x, z, 36, 3) - 0.5) * COAST;
    }

    public static double warpReach() {
        return WARP;
    }
}
