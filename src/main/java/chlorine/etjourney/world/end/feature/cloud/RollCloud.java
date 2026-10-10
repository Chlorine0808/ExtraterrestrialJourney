package chlorine.etjourney.world.end.feature.cloud;

import chlorine.etjourney.world.end.feature.Structure;
import chlorine.etjourney.world.end.feature.StructureProbe;
import chlorine.etjourney.world.end.noise.Hash;
import chlorine.etjourney.world.end.noise.ValueNoise;

/** ROLL_CLOUDS: a lying tube hundreds of blocks long, tapered shut at both ends and sometimes hollow. */
public final class RollCloud extends Structure {

    /** Wall a hollow tube keeps, so the 8-block density grid does not break it. */
    private static final double WALL = 8;
    /** Sideways and vertical sway of the axis. */
    private static final double SWAY = 12, LIFT = 6;

    public static final Structure.Kind<RollCloud> KIND = new Structure.Kind<RollCloud>("ROLL_CLOUDS", 320, 220) {

        @Override
        protected RollCloud compute(long seed, int cx, int cz, StructureProbe probe) {
            long s = seed ^ 0x1B6E9F3A5C2D8047L;
            if (Hash.hash01(s, cx, cz) > 0.5) return null;
            double x = Clouds.place(cx, cell, 0.3, Hash.hash01(s + 1, cx, cz));
            double z = Clouds.place(cz, cell, 0.3, Hash.hash01(s + 2, cx, cz));
            if (!Clouds.forms(s, cx, cz, probe.weight(style, x, z))) return null;
            double length = 200 + 180 * Hash.hash01(s + 3, cx, cz);
            double radius = 10 + 6 * Hash.hash01(s + 4, cx, cz);
            double yaw = Hash.hash01(s + 5, cx, cz) * Math.PI * 2;
            double y = Clouds.between(
                Math.max(70, Clouds.floor(probe, x, z) + radius),
                Math.min(200, Clouds.CEILING - radius - LIFT - 3),
                Hash.hash01(s + 6, cx, cz));
            if (Double.isNaN(y)) return null;
            boolean hollow = radius >= 14 && Hash.hash01(s + 7, cx, cz) < 0.4;
            long cellSalt = s + 31L * cx + cz;
            int steps = (int) Math.ceil(length / 4);
            double[] points = new double[(steps + 1) * 3];
            double ax = Math.cos(yaw), az = Math.sin(yaw);
            for (int i = 0; i <= steps; i++) {
                double along = (i / (double) steps - 0.5) * length;
                double side = (ValueNoise.mask(cellSalt + 8, along, 0, 120) - 0.5) * 2 * SWAY;
                double lift = (ValueNoise.mask(cellSalt + 9, along, 0, 90) - 0.5) * 2 * LIFT;
                points[i * 3] = x + ax * along - az * side;
                points[i * 3 + 1] = y + lift;
                points[i * 3 + 2] = z + az * along + ax * side;
            }
            return new RollCloud(x, z, y, points, length, radius, hollow, cellSalt);
        }
    };

    public final double length, radius;
    public final boolean hollow;
    private final double[] points;
    private final long salt;

    RollCloud(double x, double z, double y, double[] points, double length, double radius, boolean hollow, long salt) {
        super(x, z, length / 2 + SWAY + radius + 3, y - LIFT - radius - 3, y + LIFT + radius + 3);
        this.points = points;
        this.length = length;
        this.radius = radius;
        this.hollow = hollow;
        this.salt = salt;
    }

    /** The point of the axis at t of its length, as {x, y, z}. */
    public double[] axis(double t) {
        int n = points.length / 3 - 1, i = Math.min(n, (int) Math.round(t * n));
        return new double[] { points[i * 3], points[i * 3 + 1], points[i * 3 + 2] };
    }

    @Override
    protected double body(double x, double y, double z) {
        int n = points.length / 3 - 1;
        double best = Double.POSITIVE_INFINITY, bestT = 0;
        for (int i = 0; i < n; i++) {
            int a = i * 3, b = a + 3;
            double ex = points[b] - points[a], ey = points[b + 1] - points[a + 1], ez = points[b + 2] - points[a + 2];
            double px = x - points[a], py = y - points[a + 1], pz = z - points[a + 2];
            double f = Math.max(0, Math.min(1, (px * ex + py * ey + pz * ez) / (ex * ex + ey * ey + ez * ez)));
            double dx = px - ex * f, dy = py - ey * f, dz = pz - ez * f, d = dx * dx + dy * dy + dz * dz;
            if (d < best) {
                best = d;
                bestT = (i + f) / n;
            }
        }
        double dist = Math.sqrt(best);
        // Both ends taper over two radii and close.
        double taper = Math.min(1, Math.min(bestT, 1 - bestT) * length / (radius * 2));
        double r = radius * taper + (ValueNoise.noise3(salt, x, y, z, 9) - 0.5) * 4;
        double d = r - dist;
        return hollow ? Math.min(d, dist - (r - WALL)) : d;
    }
}
