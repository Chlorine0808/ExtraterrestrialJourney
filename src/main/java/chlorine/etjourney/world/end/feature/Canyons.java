package chlorine.etjourney.world.end.feature;

import chlorine.etjourney.world.end.noise.ValueNoise;
import chlorine.etjourney.world.end.noise.Warp;

/**
 * Slot canyons: narrow cuts that follow the mid contour of two warped noise fields. The distance to a contour is
 * estimated from the noise gradient, so the width stays 3-6 blocks however steep the noise is.
 */
public final class Canyons {

    public static final double MIN_DEPTH = 30, MAX_DEPTH = 60;
    /** How many times deeper a chasm cuts than a slot. */
    public static final double CHASM_SCALE = 3;
    private static final double[] SCALES = { 70, 43 };

    private Canyons() {}

    /** How deep the canyon cuts at (x, z), or 0 outside every slot. */
    public static double depth(long seed, double x, double z) {
        return slot(seed ^ 0x3F7A1C9E5B2D8064L, x, z);
    }

    /** How deep a chasm cuts at (x, z): three times a slot, on a pattern of its own, or 0 outside every chasm. */
    public static double chasmDepth(long seed, double x, double z) {
        return CHASM_SCALE * slot(seed ^ 0x6D2B8F4A1E7C3059L, x, z);
    }

    private static double slot(long s, double x, double z) {
        double[] w = Warp.warp(s, x, z, 24, 90);
        double half = 1.5 + 1.5 * ValueNoise.mask(s + 1, x, z, 150);
        double best = Double.MAX_VALUE;
        for (int i = 0; i < SCALES.length; i++) best = Math.min(best, distance(s + 10 + i, w[0], w[1], SCALES[i]));
        if (best >= half) return 0;
        double t = best / half;
        // Near-vertical walls over a slightly rounded floor.
        return (MIN_DEPTH + (MAX_DEPTH - MIN_DEPTH) * ValueNoise.mask(s + 2, x, z, 180)) * (1 - 0.25 * t * t);
    }

    /** Blocks from (x, z) to the 0.5 contour of one noise field. */
    static double distance(long seed, double x, double z, double scale) {
        double n = ValueNoise.mask(seed, x, z, scale) - 0.5;
        double gx = (ValueNoise.mask(seed, x + 1, z, scale) - ValueNoise.mask(seed, x - 1, z, scale)) / 2;
        double gz = (ValueNoise.mask(seed, x, z + 1, scale) - ValueNoise.mask(seed, x, z - 1, scale)) / 2;
        double g = Math.hypot(gx, gz);
        return g < 1e-6 ? Double.MAX_VALUE : Math.abs(n) / g;
    }
}
