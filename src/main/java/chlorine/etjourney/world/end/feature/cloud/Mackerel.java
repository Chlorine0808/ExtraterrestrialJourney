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

    public static final Structure.Kind<Mackerel> KIND = new Structure.Kind<Mackerel>("MACKEREL", 24, 12) {

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
            // Lobes as {dx, y, dz, size}: within 4 blocks of the cell's point, flattened to 0.6 of their size.
            double[] l = new double[tiers * lobes * 4];
            for (int t = 0; t < tiers; t++) {
                for (int k = 0; k < lobes; k++) {
                    int i = (t * lobes + k) * 4, salt = 10 + 10 * t + k;
                    l[i] = (Hash.hash01(s + salt, cx, cz) - 0.5) * 8;
                    l[i + 1] = y + t * TIER + (Hash.hash01(s + salt + 100, cx, cz) - 0.5) * 3;
                    l[i + 2] = (Hash.hash01(s + salt + 200, cx, cz) - 0.5) * 8;
                    l[i + 3] = 4 + 3 * Hash.hash01(s + salt + 300, cx, cz);
                }
            }
            return new Mackerel(x, z, y, tiers, l);
        }
    };

    public final double centreY;
    public final int tiers;
    private final double[] lobes;

    Mackerel(double x, double z, double y, int tiers, double[] lobes) {
        super(x, z, 12, y - 7, y + (tiers - 1) * TIER + 7);
        this.centreY = y;
        this.tiers = tiers;
        this.lobes = lobes;
    }

    @Override
    protected double body(double x, double y, double z) {
        double best = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < lobes.length; i += 4) {
            double a = lobes[i + 3], dx = (x - centreX - lobes[i]) / a, dz = (z - centreZ - lobes[i + 2]) / a;
            double dy = (y - lobes[i + 1]) / (0.6 * a);
            best = Math.max(best, a * (1 - Math.sqrt(dx * dx + dy * dy + dz * dz)));
        }
        return best;
    }
}
