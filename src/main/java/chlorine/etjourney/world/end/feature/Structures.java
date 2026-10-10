package chlorine.etjourney.world.end.feature;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import chlorine.etjourney.world.end.noise.Hash;
import chlorine.etjourney.world.end.noise.ValueNoise;

/**
 * The free-standing structures of the newer styles: mushroom plateaus, rings, natural arches, spiral towers and
 * hollow pillars. Parts drawn as density shapes are at least 8 blocks thick so the 8-block density grid keeps them;
 * mushrooms are drawn block by block.
 */
public final class Structures {

    /** A ring only forms where its style holds at least this weight at its centre. */
    private static final double MIN_WEIGHT = 0.5;
    /** Highest block any structure reaches. */
    private static final double CEILING = 248;
    /** No structure forms closer to the origin, clear of the vanilla central area. */
    public static final double MIN_RADIUS = 1000;
    /** Base height over the void, where there is no ground to stand on. */
    private static final double VOID_BASE = 20;

    private Structures() {}

    /** Whether the structure of a cell forms at its style's weight there, thinning out across the border. */
    private static boolean forms(long s, int cx, int cz, double weight) {
        return Fade.forms(weight, Hash.hash01(s + 99, cx, cz));
    }

    public static final Structure.Kind<Mushroom> MUSHROOMS = new Structure.Kind<Mushroom>("MUSHROOMS", 72, 46) {

        @Override
        protected Mushroom compute(long seed, int cx, int cz, StructureProbe probe) {
            long s = seed ^ 0x7A3E1C59D2B4F681L;
            if (Hash.hash01(s, cx, cz) > 0.6) return null;
            double x = (cx + 0.2 + 0.6 * Hash.hash01(s + 1, cx, cz)) * cell;
            double z = (cz + 0.2 + 0.6 * Hash.hash01(s + 2, cx, cz)) * cell;
            if (!forms(s, cx, cz, probe.weight(style, x, z))) return null;
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
        280,
        true) {

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

    /**
     * Many small, thin rings scattered through RINGS regions at any angle. Their tubes are thinner than the density
     * grid resolves, so the engine draws them block by block instead of as shapes.
     */
    public static final Structure.Kind<Ring> RINGLETS = new Structure.Kind<Ring>("RINGS", 56, 40, true) {

        @Override
        protected Ring compute(long seed, int cx, int cz, StructureProbe probe) {
            long s = seed ^ 0x4B1E7D2A9C3F6058L;
            if (Hash.hash01(s, cx, cz) > 0.55) return null;
            double x = (cx + 0.15 + 0.7 * Hash.hash01(s + 1, cx, cz)) * cell;
            double z = (cz + 0.15 + 0.7 * Hash.hash01(s + 2, cx, cz)) * cell;
            if (!forms(s, cx, cz, probe.weight(style, x, z))) return null;
            double radius = 8 + 22 * Hash.hash01(s + 3, cx, cz);
            double tube = 1.5 + 2 * Hash.hash01(s + 4, cx, cz);
            double tilt = ringTilt(Math.toRadians(90 * Hash.hash01(s + 5, cx, cz)), radius, tube);
            double yaw = Hash.hash01(s + 6, cx, cz) * Math.PI * 2;
            double reachY = radius * Math.sin(tilt) + tube;
            // Float clear of the ground, mostly in the lower half of the open air.
            double ground = probe.ground(x, z);
            double low = (ground > -100 ? ground + 6 : VOID_BASE) + reachY, high = CEILING - reachY;
            if (low > high) return null;
            double y = low + Math.min(high - low, 20 + 120 * Hash.hash01(s + 7, cx, cz));
            return new Ring(x, y, z, radius, tube, tilt, yaw, null);
        }
    };

    /**
     * Crosses 20-100 blocks tall: over land most are driven into the ground at a lean, like grave markers; the rest
     * float at any angle. Their beams are drawn block by block, so edges stay sharp.
     */
    public static final Structure.Kind<Cross> CROSSES = new Structure.Kind<Cross>("CROSSES", 80, 75) {

        @Override
        protected Cross compute(long seed, int cx, int cz, StructureProbe probe) {
            long s = seed ^ 0x6E2B9D4F1A7C3058L;
            if (Hash.hash01(s, cx, cz) > 0.6) return null;
            double x = (cx + 0.2 + 0.6 * Hash.hash01(s + 1, cx, cz)) * cell;
            double z = (cz + 0.2 + 0.6 * Hash.hash01(s + 2, cx, cz)) * cell;
            if (!forms(s, cx, cz, probe.weight(style, x, z))) return null;
            // More small crosses than large ones.
            double height = 20 + 80 * Math.pow(Hash.hash01(s + 3, cx, cz), 1.5);
            double half = Math.max(1.5, height * 0.06);
            double ground = probe.ground(x, z);
            boolean grounded = ground > -100 && Hash.hash01(s + 4, cx, cz) < 0.6;
            double lean = Math.toRadians((grounded ? 25 : 70) * Hash.hash01(s + 5, cx, cz));
            double yaw = Hash.hash01(s + 6, cx, cz) * Math.PI * 2, twist = Hash.hash01(s + 7, cx, cz) * Math.PI * 2;
            double[] up = { Math.sin(lean) * Math.cos(yaw), Math.cos(lean), Math.sin(lean) * Math.sin(yaw) };
            double[] arm = normalize(cross(up, new double[] { Math.cos(twist), 0, Math.sin(twist) }));
            double[] base;
            if (grounded) {
                // A sixth of it buried.
                base = new double[] { x, ground - height * 0.16, z };
            } else {
                double reach = height * 0.6;
                double low = (ground > -100 ? ground + 6 : VOID_BASE) + reach, high = CEILING - reach;
                if (low > high) return null;
                double y = low + Math.min(high - low, 20 + 120 * Hash.hash01(s + 8, cx, cz));
                base = new double[] { x - up[0] * height / 2, y - up[1] * height / 2, z - up[2] * height / 2 };
            }
            Cross c = Cross.of(base, up, arm, height, half, grounded);
            return c.maxY() > CEILING || c.minY() < 0 ? null : c;
        }
    };

    /** Chains hanging 20-80 blocks from the underside of the land, most with a boulder at the end. */
    public static final Structure.Kind<Chain> HANGING_CHAINS = new Structure.Kind<Chain>("CHAINS", 40, 12) {

        @Override
        protected Chain compute(long seed, int cx, int cz, StructureProbe probe) {
            long s = seed ^ 0x1D5F8A3C7E2B9064L;
            if (Hash.hash01(s, cx, cz) > 0.45) return null;
            double x = (cx + 0.2 + 0.6 * Hash.hash01(s + 1, cx, cz)) * cell;
            double z = (cz + 0.2 + 0.6 * Hash.hash01(s + 2, cx, cz)) * cell;
            if (!forms(s, cx, cz, probe.weight(style, x, z))) return null;
            double under = probe.underside(x, z);
            if (under < 40) return null;
            double scale = 1 + Hash.hash01(s + 3, cx, cz);
            double length = 20 + 60 * Hash.hash01(s + 4, cx, cz);
            // Starts inside the slab, clear of the lumps on its underside but below the ground.
            double top = Math.min(under + 8, probe.ground(x, z) - 2), bottom = Math.max(12, under - length);
            if (top < under + 1) return null;
            double weight = Hash.hash01(s + 5, cx, cz) < 0.7 ? 2.5 * scale + 1 : 0;
            return new Chain(x, z, top, bottom, scale, Hash.hash01(s + 6, cx, cz) * Math.PI, weight, true);
        }
    };

    /** Huge chains running from the top of the world to the bottom, through whatever lies between. */
    public static final Structure.Kind<Chain> SKY_CHAINS = new Structure.Kind<Chain>("CHAINS", 200, 10) {

        @Override
        protected Chain compute(long seed, int cx, int cz, StructureProbe probe) {
            long s = seed ^ 0x7B3E9C1A5D2F8046L;
            if (Hash.hash01(s, cx, cz) > 0.35) return null;
            double x = (cx + 0.2 + 0.6 * Hash.hash01(s + 1, cx, cz)) * cell;
            double z = (cz + 0.2 + 0.6 * Hash.hash01(s + 2, cx, cz)) * cell;
            if (!forms(s, cx, cz, probe.weight(style, x, z))) return null;
            double scale = 2 + Hash.hash01(s + 3, cx, cz);
            return new Chain(x, z, 253, 2, scale, Hash.hash01(s + 6, cx, cz) * Math.PI, 0, false);
        }
    };

    /** Tilt no further than keeps a ring between the void floor and the ceiling. */
    private static double ringTilt(double tilt, double radius, double tube) {
        return Math.min(tilt, Math.asin(Math.max(0, Math.min(1, ((CEILING - 8) / 2 - tube) / radius))));
    }

    public static final Structure.Kind<Arch> ARCHES = new Structure.Kind<Arch>("ARCHES", 64, 52) {

        @Override
        protected Arch compute(long seed, int cx, int cz, StructureProbe probe) {
            long s = seed ^ 0x5D1E8B3F7A2C9046L;
            if (Hash.hash01(s, cx, cz) > 0.85) return null;
            double length = 40 + 50 * Hash.hash01(s + 1, cx, cz);
            double mx = (cx + 0.2 + 0.6 * Hash.hash01(s + 5, cx, cz)) * cell;
            double mz = (cz + 0.2 + 0.6 * Hash.hash01(s + 6, cx, cz)) * cell;
            if (!forms(s, cx, cz, probe.weight(style, mx, mz))) return null;
            // Both feet stand on land: try a few headings before giving up.
            double ax = 0, az = 0, bx = 0, bz = 0;
            boolean standing = false;
            for (int attempt = 0; attempt < 3 && !standing; attempt++) {
                double angle = Hash.hash01(s + 2 + 10L * attempt, cx, cz) * Math.PI * 2;
                ax = mx - Math.cos(angle) * length / 2;
                az = mz - Math.sin(angle) * length / 2;
                bx = mx + Math.cos(angle) * length / 2;
                bz = mz + Math.sin(angle) * length / 2;
                standing = probe.land(ax, az) >= 10 && probe.land(bx, bz) >= 10;
            }
            if (!standing) return null;
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
        24,
        true) {

        @Override
        protected SpiralTower compute(long seed, int cx, int cz, StructureProbe probe) {
            long s = seed ^ 0x6C2A9F4E8B1D3570L;
            if (Hash.hash01(s, cx, cz) > 0.5) return null;
            double x = (cx + 0.2 + 0.6 * Hash.hash01(s + 1, cx, cz)) * cell;
            double z = (cz + 0.2 + 0.6 * Hash.hash01(s + 2, cx, cz)) * cell;
            if (!forms(s, cx, cz, probe.weight(style, x, z))) return null;
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
            if (!forms(s, cx, cz, probe.weight(style, x, z))) return null;
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
            Arrays.<Structure.Kind<? extends Structure>>asList(RINGS, ARCHES, SPIRAL_TOWERS, HOLLOW_PILLARS));
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
            // A slab of the cap's thickness whose rim is rounded to a half circle.
            double half = thickness / 2, inner = cap - half;
            double hat = d <= inner ? Math.min(surface - y, y - (surface - thickness))
                : half - Math.hypot(d - inner, y - (surface - half));
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

        /** The point on the ring's centre line at an angle, as {x, y, z}. */
        public double[] pointOnRing(double angle) {
            double c = Math.cos(angle) * radius, s = Math.sin(angle) * radius;
            return new double[] { centreX + c * u[0] + s * v[0], y + c * u[1] + s * v[1],
                centreZ + c * u[2] + s * v[2] };
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

    public static final class Cross extends Structure {

        private final double[] base, up, arm, out;
        public final double height, half, armAt, armHalf;
        private final boolean grounded;

        private Cross(double[] base, double[] up, double[] arm, double height, double half, double[] centre,
            double footprint, double minY, double maxY, boolean grounded) {
            super(centre[0], centre[1], footprint, minY, maxY);
            this.base = base;
            this.up = up;
            this.arm = arm;
            this.out = cross(up, arm);
            this.height = height;
            this.half = half;
            this.armAt = height * 0.72;
            this.armHalf = height * 0.3;
            this.grounded = grounded;
        }

        static Cross of(double[] base, double[] up, double[] arm, double height, double half, boolean grounded) {
            double[] out = cross(up, arm);
            // A stuck cross is placed by its foot; a floating one by its middle.
            double[] centre = grounded ? new double[] { base[0], base[2] }
                : new double[] { base[0] + up[0] * height / 2, base[2] + up[2] * height / 2 };
            double reach = 0, low = Double.MAX_VALUE, high = -Double.MAX_VALUE;
            // The corners of both beams bound the cross.
            double[][] boxes = { { 0, height, half }, { height * 0.72 - half, height * 0.72 + half, height * 0.3 } };
            for (double[] box : boxes) {
                for (double u : new double[] { box[0], box[1] }) {
                    for (double a : new double[] { -box[2], box[2] }) {
                        for (double n : new double[] { -half, half }) {
                            double px = base[0] + up[0] * u + arm[0] * a + out[0] * n;
                            double py = base[1] + up[1] * u + arm[1] * a + out[1] * n;
                            double pz = base[2] + up[2] * u + arm[2] * a + out[2] * n;
                            reach = Math.max(reach, Math.hypot(px - centre[0], pz - centre[1]));
                            low = Math.min(low, py);
                            high = Math.max(high, py);
                        }
                    }
                }
            }
            return new Cross(base, up, arm, height, half, centre, reach + 1, low - 1, high + 1, grounded);
        }

        /** The point `along` blocks up the stem and `side` blocks along the arm, as {x, y, z}. */
        public double[] point(double along, double side) {
            return new double[] { base[0] + up[0] * along + arm[0] * side, base[1] + up[1] * along + arm[1] * side,
                base[2] + up[2] * along + arm[2] * side };
        }

        @Override
        public double[][] feet() {
            return grounded ? new double[][] { { base[0], base[2] } } : new double[0][];
        }

        @Override
        protected double body(double x, double y, double z) {
            double px = x - base[0], py = y - base[1], pz = z - base[2];
            double u = px * up[0] + py * up[1] + pz * up[2];
            double a = px * arm[0] + py * arm[1] + pz * arm[2];
            double n = px * out[0] + py * out[1] + pz * out[2];
            double stem = box(a, u - height / 2, n, half, height / 2, half);
            double bar = box(a, u - armAt, n, armHalf, half, half);
            return -Math.min(stem, bar);
        }

        /** Signed distance to an axis-aligned box of the given half sizes: negative inside. */
        private static double box(double x, double y, double z, double bx, double by, double bz) {
            double qx = Math.abs(x) - bx, qy = Math.abs(y) - by, qz = Math.abs(z) - bz;
            double outside = Math.sqrt(sq(Math.max(qx, 0)) + sq(Math.max(qy, 0)) + sq(Math.max(qz, 0)));
            return outside + Math.min(Math.max(qx, Math.max(qy, qz)), 0);
        }

        private static double sq(double v) {
            return v * v;
        }
    }

    /**
     * A vertical chain of links, each a hollow loop in a vertical plane; neighbouring links turn a quarter, as real
     * chain links do. A boulder may hang from the last one.
     */
    public static final class Chain extends Structure {

        final double top, bottom, scale, yaw, weight;
        private final boolean hanging;

        Chain(double x, double z, double top, double bottom, double scale, double yaw, double weight, boolean hanging) {
            super(
                x,
                z,
                1.5 * scale + wire(scale) + Math.max(weight, 0) + 1,
                whole(top, bottom, scale) - 2 * weight - 1,
                top + wire(scale) + 1);
            this.top = top;
            this.bottom = whole(top, bottom, scale);
            this.scale = scale;
            this.yaw = yaw;
            this.weight = weight;
            this.hanging = hanging;
        }

        private static double wire(double scale) {
            return 0.45 * scale + 0.35;
        }

        /** The lowest point at or above `wanted` where the chain ends with a whole link. */
        private static double whole(double top, double wanted, double scale) {
            int links = Math.max(1, (int) Math.floor((top - wanted - 5 * scale - wire(scale)) / (4 * scale)) + 1);
            return top - ((links - 1) * 4 * scale + 5 * scale) - wire(scale);
        }

        double wire() {
            return wire(scale);
        }

        /** Distance from one link's top to the next one's; links overlap by a fifth of their length. */
        double pitch() {
            return 4 * scale;
        }

        double linkLength() {
            return 5 * scale;
        }

        /** Half the width of a link, to the middle of its wire. */
        double linkWidth() {
            return 1.5 * scale;
        }

        @Override
        public double[][] feet() {
            return hanging ? new double[][] { { centreX, centreZ } } : new double[0][];
        }

        @Override
        protected double body(double x, double y, double z) {
            double px = x - centreX, pz = z - centreZ;
            double c = Math.cos(yaw), s = Math.sin(yaw);
            double a = px * c + pz * s, b = -px * s + pz * c;
            double r = wire(scale), w = linkWidth(), half = linkLength() / 2;
            double d = Double.NEGATIVE_INFINITY;
            if (y >= bottom && y <= top + r) {
                double depth = top - y;
                int i = (int) Math.floor(depth / pitch());
                // A link reaches its wire's thickness above its own start, into the previous link's span.
                for (int k = Math.max(0, i - 1); k <= i + 1; k++) {
                    double v = depth - (k * pitch() + half);
                    double u = k % 2 == 0 ? a : b, n = k % 2 == 0 ? b : a;
                    double along = Math.max(Math.abs(v) - (half - w), 0);
                    double loop = Math.hypot(u, along) - w;
                    d = Math.max(d, r - Math.hypot(loop, n));
                }
            }
            if (weight > 0) d = Math.max(d, weight - Math.sqrt(px * px + sq(y - (bottom - weight + 1)) + pz * pz));
            return d;
        }

        private static double sq(double v) {
            return v * v;
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
