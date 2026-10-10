package chlorine.etjourney.world.end.feature.cloud;

import chlorine.etjourney.world.end.feature.Structure;
import chlorine.etjourney.world.end.feature.StructureProbe;
import chlorine.etjourney.world.end.noise.Hash;

/** BILLOWS: a sheet curling 1.5 turns over its eye, pushed sideways along the wind; drawn block by block. */
public final class Billow extends Structure {

    /** Points of the curl's cross-section. */
    private static final int CURL = 24;
    private static final long SALT = 0x2D9F6B3E1A7C5048L;

    public static final Structure.Kind<Billow> KIND = new Structure.Kind<Billow>("BILLOWS", 96, 90) {

        @Override
        protected Billow compute(long seed, int cx, int cz, StructureProbe probe) {
            long s = seed ^ SALT;
            if (Hash.hash01(s, cx, cz) > 0.55) return null;
            double x = Clouds.place(cx, cell, 0.25, Hash.hash01(s + 1, cx, cz));
            double z = Clouds.place(cz, cell, 0.25, Hash.hash01(s + 2, cx, cz));
            if (!Clouds.forms(s, cx, cz, probe.weight(style, x, z))) return null;
            if (probe.land(x, z) < 10) return null;
            double radius = 12 + 12 * Hash.hash01(s + 3, cx, cz);
            double length = 60 + 60 * Hash.hash01(s + 4, cx, cz);
            double sheet = 1.5 + Hash.hash01(s + 5, cx, cz);
            // One wind for the whole End, so neighbouring waves face nearly the same way.
            double wind = Hash.hash01(s, 0, 0) * Math.PI * 2 + (Hash.hash01(s + 6, cx, cz) - 0.5) * 0.6;
            double eye = probe.ground(x, z) - 2 + radius;
            if (eye + radius + sheet + 1 > Clouds.CEILING) return null;
            return new Billow(x, z, eye, radius, length, sheet, wind);
        }
    };

    public final double eye, radius, length, wind;
    private final double sheet;
    /** The curl as {p, q} pairs across the wind and up from the eye. */
    private final double[] curl = new double[(CURL + 1) * 2];

    Billow(double x, double z, double eye, double radius, double length, double sheet, double wind) {
        super(x, z, length / 2 + radius + sheet + 1, eye - radius - sheet - 1, eye + radius + sheet + 1);
        this.eye = eye;
        this.radius = radius;
        this.length = length;
        this.sheet = sheet;
        this.wind = wind;
        for (int k = 0; k <= CURL; k++) {
            // From the bottom of the curl, 1.5 turns inwards to 0.4 of its radius.
            double f = k / (double) CURL, a = -Math.PI / 2 + f * 3 * Math.PI, rho = radius * (1 - 0.6 * f);
            curl[k * 2] = rho * Math.cos(a);
            curl[k * 2 + 1] = rho * Math.sin(a);
        }
    }

    /** World {x, y, z} of a point along the wind, across it (p) and up from the eye (q). */
    public double[] at(double along, double p, double q) {
        double c = Math.cos(wind), s = Math.sin(wind);
        return new double[] { centreX + along * c - p * s, eye + q, centreZ + along * s + p * c };
    }

    @Override
    protected double body(double x, double y, double z) {
        double dx = x - centreX, dz = z - centreZ, c = Math.cos(wind), s = Math.sin(wind);
        if (Math.abs(dx * c + dz * s) > length / 2) return -1;
        double p = -dx * s + dz * c, q = y - eye, best = Double.POSITIVE_INFINITY;
        for (int k = 0; k < CURL; k++) {
            double ax = curl[k * 2], ay = curl[k * 2 + 1], ex = curl[k * 2 + 2] - ax, ey = curl[k * 2 + 3] - ay;
            double f = Math.max(0, Math.min(1, ((p - ax) * ex + (q - ay) * ey) / (ex * ex + ey * ey)));
            best = Math.min(best, Math.hypot(p - ax - ex * f, q - ay - ey * f));
        }
        return sheet - best;
    }
}
