package chlorine.etjourney.world.end.feature.plant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import chlorine.etjourney.world.end.feature.StructureProbe;
import chlorine.etjourney.world.end.modifier.BlockSink;
import chlorine.etjourney.world.end.modifier.EndBlock;
import chlorine.etjourney.world.end.noise.Hash;

/**
 * One giant tree, shaped from the world seed, its root and the probe's ground alone so every chunk draws the same
 * tree: a hollow ribbed trunk about 20 across and 90 high that flares at the foot, long roots crawling along the
 * ground,
 * and a broad crown of branches buried in clumps of leaves.
 */
public final class GiantTree {

    /** How far below the probe's local ground the trunk carries on, to meet the real ground a few blocks off it. */
    static final int SINK = 6;
    /** The lowest local ground the trunk and roots follow, below the root's ground. */
    static final int MAX_DROP = 16;
    /** How far below the probe's local ground a root's underside carries on. */
    private static final int ROOT_SINK = 3;
    private static final int FLARE_HEIGHT = 8;
    private static final double FLARE_RADIUS = 13;
    private static final double SHELL = 3;
    private static final double RIB = 0.7;
    private static final int TAPER = 24;
    private static final double TOP_RADIUS = 6;
    /** How far below the top the branches start. */
    private static final int CROWN = 40;
    /** Turn between one branch and the next, so the tiers do not stack. */
    private static final double GOLDEN_ANGLE = Math.PI * (3 - Math.sqrt(5));
    /** Where along a branch, from the trunk out, the clumps that cover it sit. */
    private static final double[] ALONG = { 0.25, 0.5, 0.75 };
    /** The crown's dome: clumps over a flattened half-sphere from its apex down to DOME_POLAR. */
    private static final int DOME_CLUMPS = 40;
    private static final double DOME_RADIUS = 36, DOME_HEIGHT = 24, DOME_POLAR = Math.toRadians(80);
    /** Clumps wider than this get twigs inside, so no leaf is beyond Grower.LEAF_REACH of wood. */
    private static final double TWIG_FROM = 6;
    private static final double LEAF_SQUASH = 0.75;
    /** How far in from its radius a clump's edge may be ragged, as a share of the radius. */
    private static final double RAGGED = 0.18;
    /** Spacing of the points the local ground is sampled at, between which it is interpolated. */
    private static final int GROUND_STEP = 4;
    private static final int ROOT_POINTS = 25;
    /** Share of a root's length from the trunk over which it comes down the flare, filled down to the ground. */
    private static final double FOOT_SHARE = 0.25;
    /** How far above the root's ground a root may climb before it stops. */
    private static final int ROOT_RISE = 12;
    private static final long SALT = 0x5DEECE66D3A1F2B7L;
    private static final long LEAF_SALT = 0x3C6EF372FE94F82BL;

    private final StructureProbe probe;
    private final double ax, az;
    private final int base, top;
    private final double radius;
    private final int lobes, fineLobes;
    private final double phase, finePhase;
    private final double blossomShare;
    private final long leafSeed;
    /** Tubes as {x0, y0, z0, r0, x1, y1, z1, r1}: roots, then branches and twigs. */
    private final double[][] tubes;
    private final int rootTubes;
    /** The highest y of a root, above the root's ground. */
    private final int rootHigh;
    /** The lowest y of a branch or a leaf, above the root. */
    private final int crownLow;
    /** Leaf clumps as {x, y, z, radius}. */
    private final double[][] clumps;
    private final Map<Long, Double> grounds = new HashMap<>();

    public GiantTree(long seed, double[] anchor, StructureProbe probe) {
        this.probe = probe;
        ax = anchor[0];
        az = anchor[2];
        base = (int) Math.floor(anchor[1]);
        long s = seed ^ SALT
            ^ (long) Math.floor(ax) * 0x9E3779B97F4A7C15L
            ^ (long) Math.floor(az) * 0xC2B2AE3D27D4EB4FL;
        Random random = new Random(s);
        radius = 9 + random.nextDouble() * 0.8;
        top = 90 + random.nextInt(10);
        lobes = 5 + random.nextInt(3);
        fineLobes = 11 + random.nextInt(3);
        phase = random.nextDouble() * 2 * Math.PI;
        finePhase = random.nextDouble() * 2 * Math.PI;
        blossomShare = 0.034 + random.nextDouble() * 0.012;
        leafSeed = s ^ LEAF_SALT;

        int roots = 12 + random.nextInt(7), branches = 14 + random.nextInt(5);
        List<double[]> rootParts = new ArrayList<>();
        double start = random.nextDouble() * 2 * Math.PI;
        for (int i = 0; i < roots; i++) {
            double angle = start + (i + (random.nextDouble() - 0.5) * 0.5) * 2 * Math.PI / roots;
            double length = 42 + random.nextDouble() * 18, curve = (random.nextDouble() - 0.5) * 0.9;
            double sink = 1 + random.nextDouble();
            // The root snakes sideways and up and down, in and out of the ground, at its own pace each way.
            double sideAmp = 2 + random.nextDouble() * 2, sideFreq = 2 * Math.PI / (16 + random.nextDouble() * 8);
            double sidePhase = random.nextDouble() * 2 * Math.PI;
            double upAmp = 2.5 + random.nextDouble() * 1.5, upFreq = 2 * Math.PI / (12 + random.nextDouble() * 8);
            double upPhase = random.nextDouble() * 2 * Math.PI;
            double[] previous = null;
            for (int k = 0; k < ROOT_POINTS; k++) {
                double t = k / (ROOT_POINTS - 1.0), h = 7 + (length - 7) * t, a = angle + curve * t * t;
                double side = sideAmp * Math.sin(h * sideFreq + sidePhase) * Math.min(1, 4 * t);
                double x = ax + h * Math.cos(a) - side * Math.sin(a), z = az + h * Math.sin(a) + side * Math.cos(a);
                double g = probe.ground(x, z);
                // A root stops at a drop, the void or water, and before it climbs a hill.
                if (k > 0 && (g < base - MAX_DROP || g > base + ROOT_RISE)) break;
                double up = upAmp * Math.sin(h * upFreq + upPhase) * Math.min(1, 3 * t),
                    f = Math.min(1, t / FOOT_SHARE);
                double w = f * f * (3 - 2 * f);
                // From high on the flare down to the local ground, so the root crawls along a slope.
                double y = (1 - w) * (base + 5) + w * (Math.max(base - MAX_DROP, g) + 0.5 - sink + up);
                double[] point = { x, y, z, 2 - 1.1 * t };
                if (previous != null) {
                    double[] tube = Arrays.copyOf(join(previous, point), 9);
                    // Only near the foot is the root filled down to the ground; farther out it arches freely.
                    tube[8] = t <= FOOT_SHARE ? 1 : 0;
                    rootParts.add(tube);
                }
                previous = point;
            }
        }
        rootTubes = rootParts.size();
        double high = base;
        for (double[] t : rootParts) high = Math.max(high, Math.max(t[1] + t[3], t[5] + t[7]));
        rootHigh = (int) Math.ceil(high) + 1 - base;
        List<double[]> crownTubes = new ArrayList<>(), crownClumps = new ArrayList<>(), limbs = new ArrayList<>();
        double branchStart = random.nextDouble() * 2 * Math.PI;
        double apex = base + top + 4;
        for (int i = 0; i < branches; i++) {
            double angle = branchStart + i * GOLDEN_ANGLE + (random.nextDouble() - 0.5) * 0.3;
            // Spread up the crown from the lowest branch to near the top.
            double from = base + top - CROWN + (CROWN - 10) * i / (double) branches + random.nextInt(4);
            // Low branches reach farthest, so the crown spreads out like a parasol over the trunk.
            double reach = 34 - 10.0 * i / branches + random.nextDouble() * 4, leaf = 8 + random.nextDouble() * 2.5;
            // Each branch ends just under the dome, so its leaves join the dome's.
            double to = Math.min(Math.max(from + 4, domeY(apex, reach) - 4), base + top + 1);
            double sx = ax + 6 * Math.cos(angle), sz = az + 6 * Math.sin(angle);
            double ex = ax + reach * Math.cos(angle), ez = az + reach * Math.sin(angle);
            double[] limb = { sx, from, sz, 2.2, ex, to, ez, 1.3 };
            crownTubes.add(limb);
            limbs.add(limb);
            addClump(crownTubes, crownClumps, random, ex, to + 2, ez, leaf);
            // Clumps all along the branch, growing outwards, hide its wood under leaves.
            for (double along : ALONG) {
                double t = along + (random.nextDouble() - 0.5) * 0.08;
                addClump(
                    crownTubes,
                    crownClumps,
                    random,
                    sx + (ex - sx) * t,
                    from + (to - from) * t + 1.5,
                    sz + (ez - sz) * t,
                    leaf * (0.45 + 0.5 * t));
            }
        }
        limbs.add(new double[] { ax, base + top - TAPER, az, 0, ax, base + top, az, 0 });
        // One lumpy dome of overlapping clumps over the whole crown, each on a twig from the nearest branch, so the
        // leaves read as one mass rather than tufts at the branch ends.
        for (int i = 0; i < DOME_CLUMPS; i++) {
            double u = (i + 0.5) / DOME_CLUMPS, polar = Math.acos(1 - u * (1 - Math.cos(DOME_POLAR)));
            double azimuth = i * GOLDEN_ANGLE + branchStart, out = DOME_RADIUS * (0.9 + 0.2 * random.nextDouble());
            double x = ax + out * Math.sin(polar) * Math.cos(azimuth),
                z = az + out * Math.sin(polar) * Math.sin(azimuth);
            double y = apex - DOME_HEIGHT + DOME_HEIGHT * Math.cos(polar) - 2 * random.nextDouble();
            double[] from = nearest(limbs, x, y, z);
            crownTubes.add(new double[] { from[0], from[1], from[2], 1.2, x, y, z, 0.8 });
            addClump(crownTubes, crownClumps, random, x, y, z, 7.5 + random.nextDouble() * 2);
        }
        rootParts.addAll(crownTubes);
        tubes = rootParts.toArray(new double[0][]);
        clumps = crownClumps.toArray(new double[0][]);
        double low = Double.MAX_VALUE;
        for (double[] t : crownTubes) low = Math.min(low, Math.min(t[1] - t[3], t[5] - t[7]));
        for (double[] c : clumps) low = Math.min(low, c[1] - LEAF_SQUASH * c[3]);
        crownLow = (int) Math.floor(low) - 1 - base;
    }

    /** Adds a clump and, when it is wide, twigs from its centre out through it. */
    private void addClump(List<double[]> twigs, List<double[]> clumps, Random random, double x, double y, double z,
        double r) {
        double[] c = clump(x, y, z, r);
        clumps.add(c);
        if (r <= TWIG_FROM) return;
        double start = random.nextDouble() * 2 * Math.PI, length = 0.6 * r;
        for (int k = 0; k < 4; k++) {
            double a = start + k * Math.PI / 2;
            twigs.add(
                new double[] { c[0], c[1] - 1, c[2], 1.1, c[0] + length * Math.cos(a), c[1] + 0.25 * length,
                    c[2] + length * Math.sin(a), 0.7 });
        }
    }

    /** Height of the dome's surface reach out from the axis. */
    private static double domeY(double apex, double reach) {
        double r = Math.min(1, reach / DOME_RADIUS);
        return apex - DOME_HEIGHT + DOME_HEIGHT * Math.sqrt(1 - r * r);
    }

    /** The point nearest (x, y, z) on any of the tubes' axes. */
    private static double[] nearest(List<double[]> tubes, double x, double y, double z) {
        double[] best = null;
        double bestD = Double.MAX_VALUE;
        for (double[] t : tubes) {
            double vx = t[4] - t[0], vy = t[5] - t[1], vz = t[6] - t[2];
            double along = Math.max(
                0,
                Math.min(1, ((x - t[0]) * vx + (y - t[1]) * vy + (z - t[2]) * vz) / (vx * vx + vy * vy + vz * vz)));
            double px = t[0] + vx * along, py = t[1] + vy * along, pz = t[2] + vz * along;
            double d = (px - x) * (px - x) + (py - y) * (py - y) + (pz - z) * (pz - z);
            if (d < bestD) {
                bestD = d;
                best = new double[] { px, py, pz };
            }
        }
        return best;
    }

    /** Whether the tree rooted at anchor can write any height from minY to maxY - 1. */
    public static boolean reaches(double[] anchor, int minY, int maxY) {
        int base = (int) Math.floor(anchor[1]);
        return base - MAX_DROP - SINK < maxY && base + GiantTrees.HEIGHT > minY;
    }

    /** A clump centred at (x, y, z), lowered so it ends below the tree's height. */
    private double[] clump(double x, double y, double z, double r) {
        return new double[] { x, Math.min(y, base + GiantTrees.HEIGHT - 1 - LEAF_SQUASH * r), z, r };
    }

    private static double[] join(double[] a, double[] b) {
        return new double[] { a[0], a[1], a[2], a[3], b[0], b[1], b[2], b[3] };
    }

    /**
     * Draws the part of the tree inside the sink. Wood below the root's ground overwrites, so the trunk and roots join
     * the real ground; everything above goes only into air.
     */
    public void draw(BlockSink sink) {
        int reach = GiantTrees.FOOTPRINT;
        int x0 = Math.max(sink.originX(), (int) Math.floor(ax) - reach);
        int x1 = Math.min(sink.originX() + 15, (int) Math.floor(ax) + reach);
        int z0 = Math.max(sink.originZ(), (int) Math.floor(az) - reach);
        int z1 = Math.min(sink.originZ() + 15, (int) Math.floor(az) + reach);
        int y0 = Math.max(sink.minY(), base - MAX_DROP - SINK),
            y1 = Math.min(sink.maxY() - 1, base + GiantTrees.HEIGHT - 1);
        if (y0 > y1) return;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                double dx = x + 0.5 - ax, dz = z + 0.5 - az, d = Math.sqrt(dx * dx + dz * dz);
                if (d > reach) continue;
                double angle = Math.atan2(dz, dx);
                double ground = localGround(x + 0.5, z + 0.5);
                // Only the tubes and clumps over this column, so a cell need not test the whole tree.
                double px = x + 0.5, pz = z + 0.5;
                double[][] roots = nearTubes(0, rootTubes, px, pz), crown = nearTubes(rootTubes, tubes.length, px, pz);
                double[][] leaves = nearClumps(px, pz);
                for (int y = y0; y <= y1; y++) {
                    EndBlock b = block(x, y, z, d, angle, ground, roots, crown, leaves);
                    if (b == null) continue;
                    if (b == EndBlock.GROVE_LOG && y < base) sink.set(x, y, z, b);
                    else sink.place(x, y, z, b);
                }
            }
        }
    }

    /** The block at (x, y, z), d from the axis at the given angle, over the given local ground; null for none. */
    private EndBlock block(int x, int y, int z, double d, double angle, double ground, double[][] roots,
        double[][] crown, double[][] leaves) {
        int dy = y - base;
        // The trunk carries on below the lower of the root's ground and the local ground, closed at both ends.
        int bottom = (int) Math.floor(Math.min(base, ground)) - SINK;
        if (y >= bottom && dy <= top && d <= trunk(dy, angle)) {
            double inner = Double.MAX_VALUE;
            for (int k = -2; k <= 2; k++) inner = Math.min(inner, trunk(dy + k, angle));
            return d > inner - SHELL || y < bottom + 2 ? EndBlock.GROVE_LOG : null;
        }
        double px = x + 0.5, py = y + 0.5, pz = z + 0.5;
        if (dy <= rootHigh) {
            for (double[] t : roots) if (inTube(t, px, py, pz)) return EndBlock.GROVE_LOG;
            if (py >= ground - ROOT_SINK) {
                for (double[] t : roots) if (t[8] > 0 && underRoot(t, px, py, pz)) return EndBlock.GROVE_LOG;
            }
        }
        if (dy < crownLow) return null;
        for (double[] t : crown) if (inTube(t, px, py, pz)) return EndBlock.GROVE_LOG;
        for (double[] c : leaves) {
            if (inClump(c, x, y, z)) {
                return Hash.hash01(leafSeed, x, y, z) < blossomShare ? EndBlock.GROVE_BLOSSOM : EndBlock.GROVE_LEAVES;
            }
        }
        return null;
    }

    /** Tubes from..to - 1 whose bounds, widened by their radius and a root's skirt, cover the column at (px, pz). */
    private double[][] nearTubes(int from, int to, double px, double pz) {
        List<double[]> out = new ArrayList<>();
        for (int i = from; i < to; i++) {
            double[] t = tubes[i];
            double r = Math.max(t[3], t[7]) + 1;
            if (px < Math.min(t[0], t[4]) - r || px > Math.max(t[0], t[4]) + r) continue;
            if (pz < Math.min(t[2], t[6]) - r || pz > Math.max(t[2], t[6]) + r) continue;
            out.add(t);
        }
        return out.toArray(new double[0][]);
    }

    private double[][] nearClumps(double px, double pz) {
        List<double[]> out = new ArrayList<>();
        for (double[] c : clumps) {
            double dx = px - c[0], dz = pz - c[2], r = c[3] + 1;
            if (dx * dx + dz * dz <= r * r) out.add(c);
        }
        return out.toArray(new double[0][]);
    }

    /**
     * Inside the clump's ragged edge, and joined to its solid core through cells that are inside too: stepping towards
     * the centre must stay inside, so no leaf sits alone where it would decay at once.
     */
    private boolean inClump(double[] c, int x, int y, int z) {
        double core = c[3] * (1 - RAGGED);
        for (int step = 0; step < 8; step++) {
            double ex = x + 0.5 - c[0], ey = (y + 0.5 - c[1]) / LEAF_SQUASH, ez = z + 0.5 - c[2];
            double d2 = ex * ex + ey * ey + ez * ez;
            if (d2 <= core * core) return true;
            double r = c[3] * (1 - RAGGED * Hash.hash01(leafSeed + 1, x, y, z));
            if (d2 > r * r) return false;
            // One block along the axis that brings the cell nearest the centre.
            double gainX = 2 * Math.abs(ex) - 1, gainZ = 2 * Math.abs(ez) - 1;
            double gainY = (2 * Math.abs(ey) - 1 / LEAF_SQUASH) / LEAF_SQUASH;
            if (gainX >= gainY && gainX >= gainZ) x -= (int) Math.signum(ex);
            else if (gainZ >= gainY) z -= (int) Math.signum(ez);
            else y -= (int) Math.signum(ey);
        }
        return false;
    }

    /** Trunk radius at height dy above the root and the given angle: ribbed, flared at the foot, tapered at the top. */
    private double trunk(int dy, double angle) {
        if (dy > top) return 0;
        int h = Math.max(0, dy);
        double r = radius, rib = RIB;
        if (h < FLARE_HEIGHT) {
            double f = 1 - h / (double) FLARE_HEIGHT;
            r += (FLARE_RADIUS - radius) * f * f;
            rib *= 1 + 1.5 * f * f;
        } else if (h > top - TAPER) {
            r += (TOP_RADIUS - radius) * Math.pow((h - (top - TAPER)) / (double) TAPER, 1.5);
        }
        return r + rib * (0.65 * Math.sin(lobes * angle + phase + h * 0.02)
            + 0.35 * Math.sin(fineLobes * angle + finePhase - h * 0.01));
    }

    /**
     * The probe's ground at (x, z), interpolated between points on a fixed grid so every chunk sees the same, and
     * kept within MAX_DROP below the root's ground (over the void the probe gives a very low value).
     */
    private double localGround(double x, double z) {
        double gx = x / GROUND_STEP, gz = z / GROUND_STEP;
        int i = (int) Math.floor(gx), j = (int) Math.floor(gz);
        double fx = gx - i, fz = gz - j;
        double low = (1 - fx) * groundAt(i, j) + fx * groundAt(i + 1, j);
        double high = (1 - fx) * groundAt(i, j + 1) + fx * groundAt(i + 1, j + 1);
        return (1 - fz) * low + fz * high;
    }

    private double groundAt(int i, int j) {
        return grounds.computeIfAbsent(
            ((long) i << 32) ^ (j & 0xFFFFFFFFL),
            k -> Math.max(base - MAX_DROP, probe.ground(i * GROUND_STEP, j * GROUND_STEP)));
    }

    /** Whether the point lies under a root tube, so a root crossing a dip still stands on the ground. */
    private static boolean underRoot(double[] t, double px, double py, double pz) {
        double vx = t[4] - t[0], vz = t[6] - t[2];
        double wx = px - t[0], wz = pz - t[2];
        double along = Math.max(0, Math.min(1, (wx * vx + wz * vz) / (vx * vx + vz * vz)));
        double r = t[3] + (t[7] - t[3]) * along + 0.5;
        double ox = wx - vx * along, oz = wz - vz * along;
        return ox * ox + oz * oz <= r * r && py <= t[1] + (t[5] - t[1]) * along;
    }

    private static boolean inTube(double[] t, double px, double py, double pz) {
        double vx = t[4] - t[0], vy = t[5] - t[1], vz = t[6] - t[2];
        double wx = px - t[0], wy = py - t[1], wz = pz - t[2];
        double along = Math.max(0, Math.min(1, (wx * vx + wy * vy + wz * vz) / (vx * vx + vy * vy + vz * vz)));
        double r = t[3] + (t[7] - t[3]) * along;
        double ox = wx - vx * along, oy = wy - vy * along, oz = wz - vz * along;
        return ox * ox + oy * oy + oz * oz <= r * r;
    }
}
