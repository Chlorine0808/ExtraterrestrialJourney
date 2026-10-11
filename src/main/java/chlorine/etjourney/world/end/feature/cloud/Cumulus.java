package chlorine.etjourney.world.end.feature.cloud;

import chlorine.etjourney.world.end.feature.Structure;
import chlorine.etjourney.world.end.feature.StructureProbe;
import chlorine.etjourney.world.end.noise.Distance;
import chlorine.etjourney.world.end.noise.Hash;

/** CUMULUS: a cauliflower heap of merged puffs on a flat base, floating over land or void. */
public final class Cumulus extends Structure {

    public static final Structure.Kind<Cumulus> KIND = new Structure.Kind<Cumulus>("CUMULUS", 96, 92) {

        @Override
        protected Cumulus compute(long seed, int cx, int cz, StructureProbe probe) {
            long s = seed ^ 0x3A9D5E1C7B2F4068L;
            if (Hash.hash01(s, cx, cz) > 0.5) return null;
            double x = Clouds.place(cx, cell, 0.2, Hash.hash01(s + 1, cx, cz));
            double z = Clouds.place(cz, cell, 0.2, Hash.hash01(s + 2, cx, cz));
            if (!Clouds.forms(s, cx, cz, probe.weight(style, x, z))) return null;
            // Small and large heaps, some drawn out along a random direction.
            double size = 0.55 + 0.85 * Hash.hash01(s + 6, cx, cz);
            double stretch = 1 + 0.6 * Hash.hash01(s + 7, cx, cz);
            double heading = Hash.hash01(s + 8, cx, cz) * Math.PI;
            double main = (14 + 6 * Hash.hash01(s + 3, cx, cz)) * size;
            int count = 5 + (int) (7.999 * Hash.hash01(s + 4, cx, cz));
            // Puffs as {dx, dy, dz, radius} from the main puff's centre, rising up and out from it.
            double[] puffs = new double[count * 4];
            puffs[3] = main;
            double rise = main;
            for (int i = 1; i < count; i++) {
                double a = Hash.hash01(s + 10 + i, cx, cz) * Math.PI * 2;
                double d = 0.9 * main * Hash.hash01(s + 30 + i, cx, cz);
                double r = (8 + 12 * Hash.hash01(s + 70 + i, cx, cz)) * size;
                puffs[i * 4] = Math.cos(a) * d;
                puffs[i * 4 + 1] = 0.6 * main * Hash.hash01(s + 50 + i, cx, cz);
                puffs[i * 4 + 2] = Math.sin(a) * d;
                puffs[i * 4 + 3] = r;
                rise = Math.max(rise, puffs[i * 4 + 1] + r);
            }
            double lumps = 2.5 * size;
            // The flat base cuts away the lower half of the main puff.
            double drop = main * 0.5;
            double y = Clouds.between(
                Clouds.floor(probe, x, z) + drop,
                Clouds.CEILING - rise - lumps - 2,
                Hash.hash01(s + 5, cx, cz));
            if (Double.isNaN(y)) return null;
            return new Cumulus(
                x,
                y,
                z,
                puffs,
                y - drop,
                y + rise + lumps + 2,
                size,
                stretch,
                heading,
                s ^ ((long) cx << 20) ^ cz);
        }
    };

    public final double centreY, bottom, size, stretch;
    private final double[] puffs;
    private final double cos, sin;
    private final long salt;

    Cumulus(double x, double y, double z, double[] puffs, double bottom, double top, double size, double stretch,
        double heading, long salt) {
        super(x, z, reach(puffs) * stretch + 2.5 * size, bottom - 1, top);
        this.centreY = y;
        this.bottom = bottom;
        this.puffs = puffs;
        this.size = size;
        this.stretch = stretch;
        this.cos = Math.cos(heading);
        this.sin = Math.sin(heading);
        this.salt = salt;
    }

    private static double reach(double[] puffs) {
        double reach = 0;
        for (int i = 0; i < puffs.length; i += 4) {
            reach = Math.max(reach, Distance.planar(puffs[i], puffs[i + 2]) + puffs[i + 3]);
        }
        return reach + 1;
    }

    @Override
    protected double body(double x, double y, double z) {
        // Squeeze the point along the heading, which draws the heap out that way.
        double px = x - centreX, pz = z - centreZ, a = (px * cos + pz * sin) / stretch, b = -px * sin + pz * cos;
        px = a * cos - b * sin;
        pz = a * sin + b * cos;
        double d = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < puffs.length; i += 4) {
            double dx = px - puffs[i], dy = y - centreY - puffs[i + 1], dz = pz - puffs[i + 2];
            double p = puffs[i + 3] - Math.sqrt(dx * dx + dy * dy + dz * dz);
            d = i == 0 ? p : Clouds.smoothMax(d, p, 6 * size);
        }
        return Math.min(d + Clouds.ruffle(salt, x, y, z, 7 * size, 2.5 * size), y - bottom);
    }
}
