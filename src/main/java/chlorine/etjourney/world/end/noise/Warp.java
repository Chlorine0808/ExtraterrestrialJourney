package chlorine.etjourney.world.end.noise;

/** Domain warp: bends circles and lattice-aligned blobs into organic outlines. */
public final class Warp {

    private Warp() {}

    /** (x, z) moved by up to amplitude blocks along each axis, as {x, z}. */
    public static double[] warp(long seed, double x, double z, double amplitude, double scale) {
        double dx = (Fractal.fbm(seed + 101, x, z, scale, 3) - 0.5) * 2 * amplitude;
        double dz = (Fractal.fbm(seed + 202, x, z, scale, 3) - 0.5) * 2 * amplitude;
        return new double[] { x + dx, z + dz };
    }
}
