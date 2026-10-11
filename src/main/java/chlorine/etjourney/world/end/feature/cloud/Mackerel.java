package chlorine.etjourney.world.end.feature.cloud;

import chlorine.etjourney.world.end.feature.Structure;
import chlorine.etjourney.world.end.feature.StructureProbe;
import chlorine.etjourney.world.end.noise.Hash;
import chlorine.etjourney.world.end.noise.ValueNoise;

/** MACKEREL: one small puff of two or three lobes per cell, on a sheet that waves around Y 120. */
public final class Mackerel extends Structure {

    /** Mean height of the sheet the puffs sit on. */
    public static final double LEVEL = 120;
    /** Height of the second tier over the first. */
    private static final double TIER = 30;
    /**
     * Farthest a lobe reaches from the cell's point: 4 * sqrt(2) of offset plus a size of 7, at 1.5 times, and lumps.
     */
    private static final double REACH = 21;
    /** Farthest the lumps move a lobe's surface. */
    private static final double LUMPS = 1;

    public static final Structure.Kind<Mackerel> KIND = new Structure.Kind<Mackerel>("MACKEREL", 24, REACH) {

        @Override
        protected Mackerel compute(long seed, int cx, int cz, StructureProbe probe) {
            long s = seed ^ 0x3C7F1A5E9B2D4086L;
            double x = Clouds.place(cx, cell, 0.25, Hash.hash01(s + 1, cx, cz));
            double z = Clouds.place(cz, cell, 0.25, Hash.hash01(s + 2, cx, cz));
            if (!Clouds.forms(s, cx, cz, probe.weight(style, x, z))) return null;
            double y = LEVEL + (ValueNoise.mask(s + 3, x, z, 300) - 0.5) * 40;
            if (probe.ground(x, z) > y - 10) return null;
            int tiers = Hash.hash01(s + 4, cx, cz) < 0.3 ? 2 : 1;
            int lobes = Hash.hash01(s + 5, cx, cz) < 0.5 ? 3 : 2;
            // Larger and smaller puffs, some rounder than others.
            double size = 0.6 + 0.9 * Hash.hash01(s + 6, cx, cz);
            double flatten = 0.45 + 0.35 * Hash.hash01(s + 7, cx, cz);
            // Lobes as {dx, y, dz, size}: within 4 blocks of the cell's point, both scaled by the puff's size.
            double[] l = new double[tiers * lobes * 4];
            for (int t = 0; t < tiers; t++) {
                for (int k = 0; k < lobes; k++) {
                    int i = (t * lobes + k) * 4, salt = 10 + 10 * t + k;
                    l[i] = (Hash.hash01(s + salt, cx, cz) - 0.5) * 8 * size;
                    l[i + 1] = y + t * TIER + (Hash.hash01(s + salt + 100, cx, cz) - 0.5) * 3;
                    l[i + 2] = (Hash.hash01(s + salt + 200, cx, cz) - 0.5) * 8 * size;
                    l[i + 3] = (4 + 3 * Hash.hash01(s + salt + 300, cx, cz)) * size;
                }
            }
            return new Mackerel(x, z, y, tiers, l, size, flatten, s ^ ((long) cx << 20) ^ cz);
        }
    };

    public final double centreY, size, flatten;
    public final int tiers;
    private final double[] lobes;
    private final long salt;

    Mackerel(double x, double z, double y, int tiers, double[] lobes, double size, double flatten, long salt) {
        super(x, z, reach(lobes), low(lobes, flatten), high(lobes, flatten));
        this.centreY = y;
        this.tiers = tiers;
        this.lobes = lobes;
        this.size = size;
        this.flatten = flatten;
        this.salt = salt;
    }

    private static double reach(double[] lobes) {
        double reach = 0;
        for (int i = 0; i < lobes.length; i += 4) {
            reach = Math.max(reach, Math.hypot(lobes[i], lobes[i + 2]) + lobes[i + 3]);
        }
        return reach + LUMPS + 1;
    }

    private static double low(double[] lobes, double flatten) {
        double low = Double.MAX_VALUE;
        for (int i = 0; i < lobes.length; i += 4) low = Math.min(low, lobes[i + 1] - lobes[i + 3] * flatten);
        return low - LUMPS - 1;
    }

    private static double high(double[] lobes, double flatten) {
        double high = -Double.MAX_VALUE;
        for (int i = 0; i < lobes.length; i += 4) high = Math.max(high, lobes[i + 1] + lobes[i + 3] * flatten);
        return high + LUMPS + 1;
    }

    @Override
    protected double body(double x, double y, double z) {
        double best = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < lobes.length; i += 4) {
            double a = lobes[i + 3], dx = (x - centreX - lobes[i]) / a, dz = (z - centreZ - lobes[i + 2]) / a;
            double dy = (y - lobes[i + 1]) / (flatten * a);
            best = Math.max(best, a * (1 - Math.sqrt(dx * dx + dy * dy + dz * dz)));
        }
        return best + Clouds.ruffle(salt, x, y, z, 4, LUMPS);
    }
}
