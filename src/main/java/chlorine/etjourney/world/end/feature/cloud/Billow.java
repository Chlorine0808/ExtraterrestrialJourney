package chlorine.etjourney.world.end.feature.cloud;

import chlorine.etjourney.world.end.feature.Structure;
import chlorine.etjourney.world.end.feature.StructureProbe;
import chlorine.etjourney.world.end.noise.Distance;
import chlorine.etjourney.world.end.noise.Hash;
import chlorine.etjourney.world.end.noise.ValueNoise;

/**
 * BILLOWS: a floating sheet curling over its eye, lying along its wind and narrowing to its ends; drawn block by block.
 */
public final class Billow extends Structure {

    /** Points of the curl's cross-section. */
    private static final int CURL = 24;
    private static final long SALT = 0x2D9F6B3E1A7C5048L;
    /** Largest swell of the curl along its length, as a share of its radius. */
    private static final double SWELL = 0.15;

    public static final Structure.Kind<Billow> KIND = new Structure.Kind<Billow>("BILLOWS", 96, 103) {

        {
            // Half of them break up, as real billows do.
            worn(0.5, 6);
        }

        @Override
        protected Billow compute(long seed, int cx, int cz, StructureProbe probe) {
            long s = seed ^ SALT;
            if (Hash.hash01(s, cx, cz) > 0.55) return null;
            double x = Clouds.place(cx, cell, 0.25, Hash.hash01(s + 1, cx, cz));
            double z = Clouds.place(cz, cell, 0.25, Hash.hash01(s + 2, cx, cz));
            if (!Clouds.forms(s, cx, cz, probe.weight(style, x, z))) return null;
            double radius = 8 + 18 * Hash.hash01(s + 3, cx, cz);
            double length = 30 + 110 * Hash.hash01(s + 4, cx, cz);
            double turns = 1.1 + 0.65 * Hash.hash01(s + 8, cx, cz);
            double sheet = 0.7 + 0.5 * Hash.hash01(s + 5, cx, cz);
            double wind = Hash.hash01(s + 6, cx, cz) * Math.PI * 2;
            double reach = radius * (1 + SWELL) + sheet + 1;
            double eye = Clouds
                .between(Clouds.floor(probe, x, z) + reach, Clouds.CEILING - reach, Hash.hash01(s + 7, cx, cz));
            if (Double.isNaN(eye)) return null;
            return new Billow(x, z, eye, radius, length, sheet, wind, turns, s ^ ((long) cx << 20) ^ cz);
        }
    };

    public final double eye, radius, length, wind, turns;
    private final double sheet;
    private final long salt;
    /** The curl as {p, q} pairs across the wind and up from the eye. */
    private final double[] curl = new double[(CURL + 1) * 2];

    Billow(double x, double z, double eye, double radius, double length, double sheet, double wind, double turns,
        long salt) {
        super(
            x,
            z,
            length / 2 + radius * (1 + SWELL) + sheet + 1,
            eye - radius * (1 + SWELL) - sheet - 1,
            eye + radius * (1 + SWELL) + sheet + 1);
        this.eye = eye;
        this.radius = radius;
        this.length = length;
        this.sheet = sheet;
        this.wind = wind;
        this.turns = turns;
        this.salt = salt;
        for (int k = 0; k <= CURL; k++) {
            // From the bottom of the curl, inwards to 0.4 of its radius.
            double f = k / (double) CURL, a = -Math.PI / 2 + f * turns * 2 * Math.PI, rho = radius * (1 - 0.6 * f);
            curl[k * 2] = rho * Math.cos(a);
            curl[k * 2 + 1] = rho * Math.sin(a);
        }
    }

    /** Radius of the curl at a point along the wind: narrowing to the ends and swelling here and there. */
    public double radiusAt(double along) {
        return radius * scaleAt(along);
    }

    private double scaleAt(double along) {
        double t = 2 * along / length;
        return (1 - 0.5 * t * t) * (1 + SWELL * (2 * ValueNoise.mask(salt, along, 0, 20) - 1));
    }

    /** World {x, y, z} of a point along the wind, across it (p) and up from the eye (q). */
    public double[] at(double along, double p, double q) {
        double c = Math.cos(wind), s = Math.sin(wind);
        return new double[] { centreX + along * c - p * s, eye + q, centreZ + along * s + p * c };
    }

    @Override
    public double[][] feet() {
        return new double[0][];
    }

    @Override
    protected double body(double x, double y, double z) {
        double dx = x - centreX, dz = z - centreZ, c = Math.cos(wind), s = Math.sin(wind);
        double along = dx * c + dz * s;
        if (Math.abs(along) > length / 2) return -1;
        double f = scaleAt(along), p = (-dx * s + dz * c) / f, q = (y - eye) / f;
        // The curl stays within its radius of the eye, so anything farther is clear of the sheet.
        if (Math.abs(p) > radius + sheet / f || Math.abs(q) > radius + sheet / f) return -1;
        double best = Double.POSITIVE_INFINITY;
        for (int k = 0; k < CURL; k++) {
            double ax = curl[k * 2], ay = curl[k * 2 + 1], ex = curl[k * 2 + 2] - ax, ey = curl[k * 2 + 3] - ay;
            double t = Math.max(0, Math.min(1, ((p - ax) * ex + (q - ay) * ey) / (ex * ex + ey * ey)));
            best = Math.min(best, Distance.planar(p - ax - ex * t, q - ay - ey * t));
        }
        // Back to blocks, so the sheet keeps its thickness however the curl is scaled.
        return sheet - best * f;
    }
}
