package chlorine.etjourney.world.end.feature;

import chlorine.etjourney.world.end.noise.Fractal;
import chlorine.etjourney.world.end.noise.ValueNoise;

/** Surface noise terms of the continents: base level, hills and mountain ridges. */
public final class Relief {

    public static final long SALT = 0x1F83D9ABFB41BD6BL;

    private Relief() {}

    /** Ground level: broad (520) and medium (240) terms, read at warped coordinates. */
    public static double level(long seed, double wx, double wz) {
        long s = seed ^ SALT;
        return 44 + 48 * ValueNoise.mask(s + 4, wx, wz, 520) + (ValueNoise.mask(s + 31, wx, wz, 240) - 0.5) * 24;
    }

    /** Rolling hills before interior fading: about plus or minus 19 blocks. */
    public static double hills(long seed, double wx, double wz, double x, double z) {
        long s = seed ^ SALT;
        return (ValueNoise.mask(s + 5, wx, wz, 110) - 0.5) * 24 + (ValueNoise.mask(s + 6, wx, wz, 36) - 0.5) * 8
            + (Fractal.fbm(s + 16, x, z, 14, 3) - 0.5) * 6;
    }

    /** Ridge and gully pattern for mountain slopes, about 0 to 1. */
    public static double ridges(long seed, double x, double z) {
        long s = seed ^ SALT;
        return 0.65 * folded(s + 7, x, z, 64) + 0.35 * folded(s + 9, x, z, 22);
    }

    private static double folded(long seed, double x, double z, double scale) {
        return 1 - Math.abs(2 * ValueNoise.mask(seed, x, z, scale) - 1);
    }
}
