package chlorine.etjourney.world.atmosphere;

/** Mixes the atmospheres of the four grid points around a spot, and eases the result over time. */
public final class AtmosphereBlend {

    private AtmosphereBlend() {}

    public static Atmosphere at(AtmosphereGrid grid, double x, double z) {
        return at(grid, x, z, Atmospheres.NONE);
    }

    /** As above, with bare ground standing for the given air, such as the colour vanilla computed this frame. */
    public static Atmosphere at(AtmosphereGrid grid, double x, double z, Atmosphere none) {
        double fx = x / AtmosphereGrid.CELL, fz = z / AtmosphereGrid.CELL;
        int px = (int) Math.floor(fx), pz = (int) Math.floor(fz);
        float tx = (float) (fx - px), tz = (float) (fz - pz);
        Atmosphere a = resolve(grid.idAt(px, pz), none), b = resolve(grid.idAt(px + 1, pz), none);
        Atmosphere c = resolve(grid.idAt(px, pz + 1), none), d = resolve(grid.idAt(px + 1, pz + 1), none);
        float wa = (1 - tx) * (1 - tz), wb = tx * (1 - tz), wc = (1 - tx) * tz, wd = tx * tz;
        return new Atmosphere(
            wa * a.r + wb * b.r + wc * c.r + wd * d.r,
            wa * a.g + wb * b.g + wc * c.g + wd * d.g,
            wa * a.b + wb * b.b + wc * c.b + wd * d.b,
            wa * a.density + wb * b.density + wc * c.density + wd * d.density,
            wa * a.skyMix + wb * b.skyMix + wc * c.skyMix + wd * d.skyMix);
    }

    private static Atmosphere resolve(byte id, Atmosphere none) {
        Atmosphere a = Atmospheres.byId(id);
        return a == Atmospheres.NONE ? none : a;
    }

    /** The ID weighing most at (x, z), for effects that pick one atmosphere rather than mix them. */
    public static byte dominant(AtmosphereGrid grid, double x, double z) {
        double fx = x / AtmosphereGrid.CELL, fz = z / AtmosphereGrid.CELL;
        int px = (int) Math.floor(fx), pz = (int) Math.floor(fz);
        double tx = fx - px, tz = fz - pz;
        byte[] ids = { grid.idAt(px, pz), grid.idAt(px + 1, pz), grid.idAt(px, pz + 1), grid.idAt(px + 1, pz + 1) };
        double[] w = { (1 - tx) * (1 - tz), tx * (1 - tz), (1 - tx) * tz, tx * tz };
        byte best = ids[0];
        double bestWeight = -1;
        for (int i = 0; i < 4; i++) {
            double sum = 0;
            for (int k = 0; k < 4; k++) if (ids[k] == ids[i]) sum += w[k];
            if (sum > bestWeight) {
                bestWeight = sum;
                best = ids[i];
            }
        }
        return best;
    }

    /** Follows a target exponentially, so late packets and teleports fade instead of cutting. */
    public static final class Easing {

        public static final double HALF_LIFE = 0.5;

        private Atmosphere current;

        public Atmosphere step(Atmosphere target, double seconds) {
            if (current == null) return current = target;
            float k = (float) (1 - Math.pow(0.5, seconds / HALF_LIFE));
            current = new Atmosphere(
                current.r + (target.r - current.r) * k,
                current.g + (target.g - current.g) * k,
                current.b + (target.b - current.b) * k,
                current.density + (target.density - current.density) * k,
                current.skyMix + (target.skyMix - current.skyMix) * k);
            return current;
        }

        public void reset() {
            current = null;
        }
    }
}
