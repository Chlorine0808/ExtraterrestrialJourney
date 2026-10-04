package chlorine.etjourney.world.end.feature;

import chlorine.etjourney.world.end.noise.ValueNoise;

/** The underside of the continent slab, and keeping the slab inside the world. */
public final class Underside {

    public static final double MIN_BOTTOM = 6, MAX_BOTTOM = 110;

    private Underside() {}

    /**
     * Depth of the underside below ground level. Normalised distance from the rim is bent by a superellipse whose
     * exponent drifts between 1 (cone), 2 (half sphere) and 3 (flat bowl); thickness drifts between 0.5 and 0.8 of
     * the first spike's.
     */
    public static double depth(long seed, double land, double x, double z) {
        long s = seed ^ Relief.SALT;
        double u = Math.min(1, Math.max(0, land / Continent.MAX_HEIGHT));
        double a = 1 + 2 * ValueNoise.mask(s + 10, x, z, 360);
        double bent = Math.pow(1 - Math.pow(1 - u, a), 1 / a);
        double scale = 0.5 + 0.3 * ValueNoise.mask(s + 15, x, z, 300);
        return (3 + 36 * bent * (0.7 + 0.6 * ValueNoise.mask(s + 8, x, z, 40))) * scale;
    }

    /** Shifts a column so its bottom lies within [MIN_BOTTOM, MAX_BOTTOM], keeping its thickness. */
    public static double shift(double bottom) {
        return Math.max(0, MIN_BOTTOM - bottom) - Math.max(0, bottom - MAX_BOTTOM);
    }
}
