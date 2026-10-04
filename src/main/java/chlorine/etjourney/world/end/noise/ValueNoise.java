package chlorine.etjourney.world.end.noise;

/** Smoothstep-interpolated value noise in [0, 1]. */
public final class ValueNoise {

    private ValueNoise() {}

    public static double mask(long seed, double x, double z, double scale) {
        double fx = x / scale, fz = z / scale;
        int x0 = (int) Math.floor(fx), z0 = (int) Math.floor(fz);
        double tx = smooth(fx - x0), tz = smooth(fz - z0);
        double a = lerp(Hash.hash01(seed, x0, z0), Hash.hash01(seed, x0 + 1, z0), tx);
        double b = lerp(Hash.hash01(seed, x0, z0 + 1), Hash.hash01(seed, x0 + 1, z0 + 1), tx);
        return lerp(a, b, tz);
    }

    public static double noise3(long seed, double x, double y, double z, double scale) {
        double fx = x / scale, fy = y / scale, fz = z / scale;
        int x0 = (int) Math.floor(fx), y0 = (int) Math.floor(fy), z0 = (int) Math.floor(fz);
        double tx = smooth(fx - x0), ty = smooth(fy - y0), tz = smooth(fz - z0);
        double result = 0;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 2; k++) {
                    double v = Hash.hash01(seed, x0 + i, y0 + j, z0 + k);
                    result += v * (i == 0 ? 1 - tx : tx) * (j == 0 ? 1 - ty : ty) * (k == 0 ? 1 - tz : tz);
                }
            }
        }
        return result;
    }

    public static double smooth(double t) {
        return t * t * (3 - 2 * t);
    }

    static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
