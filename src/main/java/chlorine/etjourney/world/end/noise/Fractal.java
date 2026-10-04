package chlorine.etjourney.world.end.noise;

/** Fractal sums of value noise whose octaves are rotated, so their lattices never line up. */
public final class Fractal {

    /** About 37 degrees between octaves. */
    private static final double COS = Math.cos(0.65), SIN = Math.sin(0.65);
    private static final double LACUNARITY = 2.03;
    private static final long OCTAVE_SALT = 0x9E3779B9L;

    private Fractal() {}

    /** Normalised to about [0, 1]. */
    public static double fbm(long seed, double x, double z, double scale, int octaves) {
        return sum(seed, x, z, scale, octaves, false);
    }

    /** Each octave folded at its middle value, so the sum forms long sharp crests. About [0, 1]. */
    public static double ridged(long seed, double x, double z, double scale, int octaves) {
        return sum(seed, x, z, scale, octaves, true);
    }

    private static double sum(long seed, double x, double z, double scale, int octaves, boolean ridged) {
        double sum = 0, amplitude = 1, total = 0;
        for (int i = 0; i < octaves; i++) {
            double v = ValueNoise.mask(seed + i * OCTAVE_SALT, x, z, scale);
            if (ridged) {
                double r = 1 - Math.abs(2 * v - 1);
                v = r * r;
            }
            sum += amplitude * v;
            total += amplitude;
            double rx = x * COS - z * SIN, rz = x * SIN + z * COS;
            x = rx * LACUNARITY;
            z = rz * LACUNARITY;
            amplitude *= 0.5;
        }
        return sum / total;
    }
}
