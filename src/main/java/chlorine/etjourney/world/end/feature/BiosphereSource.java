package chlorine.etjourney.world.end.feature;

import chlorine.etjourney.world.end.noise.Hash;

/** Where a biosphere's terrain comes from: the kind of world and the point around which it is cut. */
public final class BiosphereSource {

    public enum Kind {
        SURFACE,
        CAVE,
        NETHER
    }

    /** Whether the biome map puts an ocean, a river or a beach at (x, z). */
    public interface Water {

        boolean at(int x, int z);
    }

    /** Shares of balls cut from the Nether, and of the others cut from a cave. */
    public static final double NETHER_SHARE = 0.15, CAVE_SHARE = 0.10;
    /** Share of surface balls that keep their first point even on water. */
    public static final double WATER_SHARE = 0.15;
    /** Points lie within this many blocks of the origin; an eighth of it in the Nether. */
    public static final int SPREAD = 100000;
    private static final int TRIES = 16;
    private static final long SALT = 0x5E3A9C1F7B2D4068L;

    private BiosphereSource() {}

    public static Kind kind(long seed, Biosphere b) {
        long s = seed ^ SALT;
        int cx = (int) Math.floor(b.centreX), cz = (int) Math.floor(b.centreZ);
        if (Hash.hash01(s, cx, cz) < NETHER_SHARE) return Kind.NETHER;
        return Hash.hash01(s + 1, cx, cz) < CAVE_SHARE ? Kind.CAVE : Kind.SURFACE;
    }

    /** The kinds to try for a ball, its own first: the overworld and the Nether stand in for each other. */
    public static Kind[] order(Kind kind) {
        return new Kind[] { kind, kind == Kind.NETHER ? Kind.SURFACE : Kind.NETHER };
    }

    /** The point, {x, z}, the ball's terrain is cut around; surface balls mostly try until one is not water. */
    public static int[] point(long seed, Biosphere b, Kind kind, Water water) {
        long s = seed ^ SALT;
        int cx = (int) Math.floor(b.centreX), cz = (int) Math.floor(b.centreZ);
        boolean anyWater = kind != Kind.SURFACE || Hash.hash01(s + 2, cx, cz) < WATER_SHARE;
        int spread = kind == Kind.NETHER ? SPREAD / 8 : SPREAD;
        int[] p = null;
        for (int t = 0; t < TRIES; t++) {
            p = new int[] { (int) ((Hash.hash01(s + 3 + 2L * t, cx, cz) * 2 - 1) * spread),
                (int) ((Hash.hash01(s + 4 + 2L * t, cx, cz) * 2 - 1) * spread) };
            if (anyWater || !water.at(p[0], p[1])) return p;
        }
        return p;
    }
}
