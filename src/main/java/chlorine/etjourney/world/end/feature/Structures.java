package chlorine.etjourney.world.end.feature;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import chlorine.etjourney.world.end.noise.Hash;
import chlorine.etjourney.world.end.noise.ValueNoise;

/**
 * The free-standing structures of the newer styles: mushroom plateaus, rings, natural arches, spiral towers and
 * hollow pillars. Every part is at least 8 blocks thick so the 8-block density grid keeps it.
 */
public final class Structures {

    /** A structure only forms where its style holds at least this weight at its centre. */
    private static final double MIN_WEIGHT = 0.5;
    /** Highest block any structure reaches. */
    private static final double CEILING = 248;
    /** No structure forms closer to the origin, clear of the vanilla central area. */
    public static final double MIN_RADIUS = 1000;
    /** Base height over the void, where there is no ground to stand on. */
    private static final double VOID_BASE = 20;

    private Structures() {}

    public static final Structure.Kind<Mushroom> MUSHROOMS = new Structure.Kind<Mushroom>("MUSHROOMS", 72, 46) {

        @Override
        protected Mushroom compute(long seed, int cx, int cz, StructureProbe probe) {
            long s = seed ^ 0x7A3E1C59D2B4F681L;
            if (Hash.hash01(s, cx, cz) > 0.6) return null;
            double x = (cx + 0.2 + 0.6 * Hash.hash01(s + 1, cx, cz)) * cell;
            double z = (cz + 0.2 + 0.6 * Hash.hash01(s + 2, cx, cz)) * cell;
            if (probe.weight(style, x, z) < MIN_WEIGHT) return null;
            double base = base(probe, x, z, Hash.hash01(s + 3, cx, cz));
            double capY = Math.min(CEILING - 14, base + 30 + 80 * Hash.hash01(s + 4, cx, cz));
            if (capY - base < 20) return null;
            double stalk = 5 + 3 * Hash.hash01(s + 5, cx, cz);
            double cap = 18 + 27 * Hash.hash01(s + 6, cx, cz);
            double thickness = 5 + 3 * Hash.hash01(s + 7, cx, cz);
            double dome = 4 + 6 * Hash.hash01(s + 8, cx, cz);
            return new Mushroom(x, z, base, capY, stalk, cap, thickness, dome);
        }
    };

    /** One ring per RINGS region, on the region's own grid, where the style is strongest near the region centre. */
    public static final Structure.Kind<Ring> RINGS = new Structure.Kind<Ring>(
        "RINGS",
        StructureProbe.REGION_CELL,
        280) {

        @Override
        protected Ring compute(long seed, int cx, int cz, StructureProbe probe) {
            long s = seed ^ 0x2F7C4A9E1B3D5860L;
            double[] centre = probe.regionCentre(cx, cz);
            // The region map is warped: look around the centre for where the style holds, staying inside the cell.
            // Ties keep the centre, which comes first.
            double x = centre[0], z = centre[1], best = probe.weight(style, x, z);
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    if (i == 0 && j == 0) continue;
                    double px = centre[0] + i * 100, pz = centre[1] + j * 100;
                    double w = probe.weight(style, px, pz);
                    if (w > best) {
                        best = w;
                        x = px;
                        z = pz;
                    }
                }
            }
            if (best < MIN_WEIGHT || Math.hypot(x, z) < 1400) return null;
            double radius = 120 + 140 * Hash.hash01(s + 3, cx, cz);
            double tube = 7 + 5 * Hash.hash01(s + 4, cx, cz);
            double tilt = ringTilt(Math.toRadians(35 * Hash.hash01(s + 5, cx, cz)), radius, tube);
            double yaw = Hash.hash01(s + 6, cx, cz) * Math.PI * 2;
            // Sometimes a second, smaller ring on the same centre at another angle, like a gyroscope.
            boolean gyro = Hash.hash01(s + 8, cx, cz) < 0.4;
            double inner = radius * (0.5 + 0.2 * Hash.hash01(s + 9, cx, cz)), innerTube = tube * 0.8;
            double innerTilt = ringTilt(Math.toRadians(40 + 50 * Hash.hash01(s + 10, cx, cz)), inner, innerTube);
            double reachY = radius * Math.sin(tilt) + tube;
            if (gyro) reachY = Math.max(reachY, inner * Math.sin(innerTilt) + innerTube);
            double y = Math.max(reachY + 4, Math.min(CEILING - reachY, 60 + 120 * Hash.hash01(s + 7, cx, cz)));
            Ring second = gyro ? new Ring(x, y, z, inner, innerTube, innerTilt, yaw + Math.PI / 2, null) : null;
            return new Ring(x, y, z, radius, tube, tilt, yaw, second);
        }
    };

    /** Tilt no further than keeps a ring between the void floor and the ceiling. */
    private static double ringTilt(double tilt, double radius, double tube) {
        return Math.min(tilt, Math.asin(Math.max(0, Math.min(1, ((CEILING - 8) / 2 - tube) / radius))));
    }

    public static final Structure.Kind<Arch> ARCHES = new Structure.Kind<Arch>("ARCHES", 96, 52) {

        @Override
        protected Arch compute(long seed, int cx, int cz, StructureProbe probe) {
            long s = seed ^ 0x5D1E8B3F7A2C9046L;
            if (Hash.hash01(s, cx, cz) > 0.7) return null;
            double length = 40 + 50 * Hash.hash01(s + 1, cx, cz);
            double angle = Hash.hash01(s + 2, cx, cz) * Math.PI * 2;
            double mx = (cx + 0.5) * cell, mz = (cz + 0.5) * cell;
            if (probe.weight(style, mx, mz) < MIN_WEIGHT) return null;
            double ax = mx - Math.cos(angle) * length / 2, az = mz - Math.sin(angle) * length / 2;
            double bx = mx + Math.cos(angle) * length / 2, bz = mz + Math.sin(angle) * length / 2;
            // Both feet stand on land.
            if (probe.land(ax, az) < 10 || probe.land(bx, bz) < 10) return null;
            double ya = probe.ground(ax, az) - 2, yb = probe.ground(bx, bz) - 2;
            double height = 15 + 25 * Hash.hash01(s + 3, cx, cz);
            double tube = 4.5 + 2.5 * Hash.hash01(s + 4, cx, cz);
            int steps = (int) Math.ceil(length / 4);
            double[] points = new double[(steps + 1) * 3];
            for (int i = 0; i <= steps; i++) {
                double t = i / (double) steps;
                points[i * 3] = ax + (bx - ax) * t;
                points[i * 3 + 1] = Math.min(CEILING - tube, ya + (yb - ya) * t + height * Math.sin(Math.PI * t));
                points[i * 3 + 2] = az + (bz - az) * t;
            }
            return new Arch(mx, mz, length / 2 + tube, points, tube);
        }
    };

    public static final Structure.Kind<SpiralTower> SPIRAL_TOWERS = new Structure.Kind<SpiralTower>(
        "SPIRAL_TOWERS",
        160,
        24) {

        @Override
        protected SpiralTower compute(long seed, int cx, int cz, StructureProbe probe) {
            long s = seed ^ 0x6C2A9F4E8B1D3570L;
            if (Hash.hash01(s, cx, cz) > 0.5) return null;
            double x = (cx + 0.2 + 0.6 * Hash.hash01(s + 1, cx, cz)) * cell;
            double z = (cz + 0.2 + 0.6 * Hash.hash01(s + 2, cx, cz)) * cell;
            if (probe.weight(style, x, z) < MIN_WEIGHT) return null;
            double base = base(probe, x, z, Hash.hash01(s + 3, cx, cz));
            double ribbon = 4;
            double top = Math.min(CEILING - ribbon, base + 150 + 90 * Hash.hash01(s + 4, cx, cz));
            if (top - base < 60) return null;
            double pillar = 6 + 4 * Hash.hash01(s + 5, cx, cz);
            double helix = pillar + 7;
            double pitch = 22 + 8 * Hash.hash01(s + 6, cx, cz);
            double phase = Hash.hash01(s + 7, cx, cz) * Math.PI * 2;
            // The ramp: a tube winding around the pillar, one turn per pitch blocks of height.
            int steps = (int) Math.ceil((top - base) / pitch * (2 * Math.PI * helix) / 4);
            double[] points = new double[(steps + 1) * 3];
            for (int i = 0; i <= steps; i++) {
                double y = base + (top - base) * i / (double) steps;
                double a = phase + (y - base) / pitch * Math.PI * 2;
                points[i * 3] = x + Math.cos(a) * helix;
                points[i * 3 + 1] = y;
                points[i * 3 + 2] = z + Math.sin(a) * helix;
            }
            return new SpiralTower(x, z, base, top, pillar, helix + ribbon, points, ribbon);
        }
    };

    public static final Structure.Kind<HollowPillar> HOLLOW_PILLARS = new Structure.Kind<HollowPillar>(
        "HOLLOW_PILLARS",
        120,
        36) {

        @Override
        protected HollowPillar compute(long seed, int cx, int cz, StructureProbe probe) {
            long s = seed ^ 0x1E9B5D3A7C2F4086L;
            if (Hash.hash01(s, cx, cz) > 0.5) return null;
            double x = (cx + 0.25 + 0.5 * Hash.hash01(s + 1, cx, cz)) * cell;
            double z = (cz + 0.25 + 0.5 * Hash.hash01(s + 2, cx, cz)) * cell;
            if (probe.weight(style, x, z) < MIN_WEIGHT) return null;
            double base = base(probe, x, z, Hash.hash01(s + 3, cx, cz));
            double top = Math.min(CEILING, base + 80 + 120 * Hash.hash01(s + 4, cx, cz));
            if (top - base < 40) return null;
            double radius = 12 + 13 * Hash.hash01(s + 5, cx, cz);
            double wall = 8 + 2 * Hash.hash01(s + 6, cx, cz);
            return new HollowPillar(x, z, base, top, radius, wall, s ^ ((long) cx << 20) ^ cz);
        }
    };

    public static List<Structure.Kind<? extends Structure>> kinds() {
        return Collections.unmodifiableList(
            Arrays.<Structure.Kind<? extends Structure>>asList(
                MUSHROOMS,
                RINGS,
                ARCHES,
                SPIRAL_TOWERS,
                HOLLOW_PILLARS));
    }

    /** Ground to stand on (slightly sunk in), or a random low height over the void. */
    private static double base(StructureProbe probe, double x, double z, double roll) {
        double ground = probe.ground(x, z);
        return ground > -100 ? ground - 4 : VOID_BASE + 40 * roll;
    }

    public static final class Mushroom extends Structure {

        final double base, capY, stalk, cap, thickness, dome;

        Mushroom(double x, double z, double base, double capY, double stalk, double cap, double thickness,
            double dome) {
            super(x, z, cap + 1, base - 1, capY + dome + 1);
            this.base = base;
            this.capY = capY;
            this.stalk = stalk;
            this.cap = cap;
            this.thickness = thickness;
            this.dome = dome;
        }

        @Override
        protected double body(double x, double y, double z) {
            double d = Math.hypot(x - centreX, z - centreZ);
            // The stalk runs up into the cap's underside, which is highest on the axis.
            double stem = Math.min(stalk - d, Math.min(capY + dome - thickness + 1 - y, y - base));
            double t = Math.min(1, d / cap);
            double surface = capY + dome * (1 - t * t);
            double hat = Math.min(cap - d, Math.min(surface - y, y - (surface - thickness)));
            return Math.max(stem, hat);
        }
    }

    public static final class Ring extends Structure {

        final double y, radius, tube;
        final double[] n, u, v;
        /** A second ring on the same centre, or null. */
        final Ring inner;

        Ring(double x, double y, double z, double radius, double tube, double tilt, double yaw, Ring inner) {
            super(x, z, radius + tube, y - radius * Math.sin(tilt) - tube, y + radius * Math.sin(tilt) + tube);
            this.y = y;
            this.radius = radius;
            this.tube = tube;
            this.inner = inner;
            n = new double[] { Math.sin(tilt) * Math.cos(yaw), Math.cos(tilt), Math.sin(tilt) * Math.sin(yaw) };
            u = normalize(cross(n, Math.abs(n[1]) < 0.9 ? new double[] { 0, 1, 0 } : new double[] { 1, 0, 0 }));
            v = cross(n, u);
        }

        @Override
        public double[][] feet() {
            return new double[0][];
        }

        @Override
        public double minY() {
            return inner == null ? super.minY() : Math.min(super.minY(), inner.minY());
        }

        @Override
        public double maxY() {
            return inner == null ? super.maxY() : Math.max(super.maxY(), inner.maxY());
        }

        @Override
        protected double body(double x, double y, double z) {
            double px = x - centreX, py = y - this.y, pz = z - centreZ;
            double h = px * n[0] + py * n[1] + pz * n[2];
            double a = px * u[0] + py * u[1] + pz * u[2], b = px * v[0] + py * v[1] + pz * v[2];
            double r = Math.hypot(a, b) - radius;
            double d = tube - Math.sqrt(h * h + r * r);
            return inner == null ? d : Math.max(d, inner.body(x, y, z));
        }
    }

    public static final class Arch extends Structure {

        private final ArcPaths.Segments segments;

        private final double[][] feet;

        Arch(double x, double z, double footprint, double[] points, double tube) {
            super(x, z, footprint, low(points) - tube, high(points) + tube);
            feet = new double[][] { { points[0], points[2] },
                { points[points.length - 3], points[points.length - 1] } };
            segments = ArcPaths.segmentsNear(
                Collections.singletonList(new ArcPaths.Path(points, tube)),
                -1e9,
                1e9,
                -1e9,
                1e9,
                (a, b, c) -> true);
        }

        @Override
        public double[][] feet() {
            return feet;
        }

        @Override
        protected double body(double x, double y, double z) {
            return ArcPaths.density(segments, x, y, z);
        }
    }

    public static final class SpiralTower extends Structure {

        final double base, top, pillar;
        private final ArcPaths.Segments ramp;

        SpiralTower(double x, double z, double base, double top, double pillar, double footprint, double[] ramp,
            double ribbon) {
            super(x, z, footprint, base - 1, top + ribbon);
            this.base = base;
            this.top = top;
            this.pillar = pillar;
            this.ramp = ArcPaths.segmentsNear(
                Collections.singletonList(new ArcPaths.Path(ramp, ribbon)),
                -1e9,
                1e9,
                -1e9,
                1e9,
                (a, b, c) -> true);
        }

        @Override
        protected double body(double x, double y, double z) {
            double column = Math.min(pillar - Math.hypot(x - centreX, z - centreZ), Math.min(top - y, y - base));
            return Math.max(column, ArcPaths.density(ramp, x, y, z));
        }
    }

    public static final class HollowPillar extends Structure {

        final double base, top, radius, wall;
        private final long seed;

        HollowPillar(double x, double z, double base, double top, double radius, double wall, long seed) {
            super(x, z, radius + wall, base - 1, top + 1);
            this.base = base;
            this.top = top;
            this.radius = radius;
            this.wall = wall;
            this.seed = seed;
        }

        @Override
        protected double body(double x, double y, double z) {
            double d = Math.hypot(x - centreX, z - centreZ);
            double shell = Math.min(wall / 2 - Math.abs(d - radius), Math.min(top - y, y - base));
            // Windows: openings where a 3D noise peaks.
            double n = ValueNoise.noise3(seed, x, y, z, 9);
            return n > 0.72 ? Math.min(shell, (0.72 - n) * 40) : shell;
        }
    }

    private static double low(double[] points) {
        double low = Double.MAX_VALUE;
        for (int i = 1; i < points.length; i += 3) low = Math.min(low, points[i]);
        return low;
    }

    private static double high(double[] points) {
        double high = -Double.MAX_VALUE;
        for (int i = 1; i < points.length; i += 3) high = Math.max(high, points[i]);
        return high;
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] { a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0] };
    }

    private static double[] normalize(double[] a) {
        double l = Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
        return new double[] { a[0] / l, a[1] / l, a[2] / l };
    }
}
