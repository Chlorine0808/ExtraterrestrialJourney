package chlorine.etjourney.world.end.feature.plant;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import chlorine.etjourney.world.end.feature.StructureProbe;
import chlorine.etjourney.world.end.modifier.BlockSink;
import chlorine.etjourney.world.end.modifier.EndBlock;
import chlorine.etjourney.world.end.noise.Hash;

/**
 * One giant tree, shaped from the world seed, its root and the probe's ground alone so every chunk draws the same
 * tree: a hollow ribbed trunk about 20 across that flares at the foot, roots crawling along the ground, and a few
 * branches ending in clumps of leaves.
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
    private static final int TAPER = 12;
    private static final double TOP_RADIUS = 5;
    private static final double LEAF_SQUASH = 0.75;
    /** Spacing of the points the local ground is sampled at, between which it is interpolated. */
    private static final int GROUND_STEP = 4;
    private static final int ROOT_POINTS = 6;
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
    /** Tubes as {x0, y0, z0, r0, x1, y1, z1, r1}: roots, then branches. */
    private final double[][] tubes;
    private final int rootTubes;
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
        top = 45 + random.nextInt(5);
        lobes = 5 + random.nextInt(3);
        fineLobes = 11 + random.nextInt(3);
        phase = random.nextDouble() * 2 * Math.PI;
        finePhase = random.nextDouble() * 2 * Math.PI;
        blossomShare = 0.034 + random.nextDouble() * 0.012;
        leafSeed = s ^ LEAF_SALT;

        int roots = 6 + random.nextInt(4), branches = 4 + random.nextInt(3);
        rootTubes = roots * (ROOT_POINTS - 1);
        tubes = new double[rootTubes + branches][];
        double start = random.nextDouble() * 2 * Math.PI;
        for (int i = 0; i < roots; i++) {
            double angle = start + (i + (random.nextDouble() - 0.5) * 0.5) * 2 * Math.PI / roots;
            double length = 18 + random.nextDouble() * 5, curve = (random.nextDouble() - 0.5) * 0.7;
            double sink = 1 + random.nextDouble() * 1.5;
            double[] previous = null;
            for (int k = 0; k < ROOT_POINTS; k++) {
                double t = k / (ROOT_POINTS - 1.0), h = 7 + (length - 7) * t, a = angle + curve * t * t;
                double x = ax + h * Math.cos(a), z = az + h * Math.sin(a), w = Math.pow(t, 1.5);
                // From high on the flare down to just under the local ground, so the root crawls along a slope.
                double y = (1 - w) * (base + 5) + w * (Math.max(base - MAX_DROP, probe.ground(x, z)) + 0.5 - sink);
                double[] point = { x, y, z, 3 - 1.2 * t };
                if (previous != null) tubes[i * (ROOT_POINTS - 1) + k - 1] = join(previous, point);
                previous = point;
            }
        }
        clumps = new double[branches + 1][];
        double branchStart = random.nextDouble() * 2 * Math.PI;
        for (int i = 0; i < branches; i++) {
            double angle = branchStart + (i + (random.nextDouble() - 0.5) * 0.5) * 2 * Math.PI / branches;
            double from = base + top - 16 + random.nextInt(11);
            double to = Math.min(from + 5 + random.nextInt(5), base + top + 1);
            double reach = 12 + random.nextDouble() * 4, leaf = 5 + random.nextDouble() * 2;
            double ex = ax + reach * Math.cos(angle), ez = az + reach * Math.sin(angle);
            tubes[rootTubes + i] = new double[] { ax + 6 * Math.cos(angle), from, az + 6 * Math.sin(angle), 2, ex, to,
                ez, 1.4 };
            clumps[i] = clump(ex, to + 2, ez, leaf);
        }
        clumps[branches] = clump(ax, base + top + 1, az, 7 + random.nextDouble() * 1.5);
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
                for (int y = y0; y <= y1; y++) {
                    EndBlock b = block(x, y, z, d, angle, ground);
                    if (b == null) continue;
                    if (b == EndBlock.GROVE_LOG && y < base) sink.set(x, y, z, b);
                    else sink.place(x, y, z, b);
                }
            }
        }
    }

    /** The block at (x, y, z), d from the axis at the given angle, over the given local ground; null for none. */
    private EndBlock block(int x, int y, int z, double d, double angle, double ground) {
        int dy = y - base;
        // The trunk carries on below the lower of the root's ground and the local ground, closed at both ends.
        int bottom = (int) Math.floor(Math.min(base, ground)) - SINK;
        if (y >= bottom && dy <= top && d <= trunk(dy, angle)) {
            double inner = Double.MAX_VALUE;
            for (int k = -2; k <= 2; k++) inner = Math.min(inner, trunk(dy + k, angle));
            return d > inner - SHELL || y < bottom + 2 ? EndBlock.GROVE_LOG : null;
        }
        double px = x + 0.5, py = y + 0.5, pz = z + 0.5;
        int first = dy <= 9 ? 0 : rootTubes, last = dy >= top - 20 ? tubes.length : rootTubes;
        for (int i = first; i < last; i++) if (inTube(tubes[i], px, py, pz)) return EndBlock.GROVE_LOG;
        if (dy <= 9 && py >= ground - ROOT_SINK) {
            for (int i = 0; i < rootTubes; i++) if (underRoot(tubes[i], px, py, pz)) return EndBlock.GROVE_LOG;
        }
        if (dy < top - 25) return null;
        for (double[] c : clumps) {
            double ex = px - c[0], ey = (py - c[1]) / LEAF_SQUASH, ez = pz - c[2];
            double r = c[3] * (1 - 0.18 * Hash.hash01(leafSeed + 1, x, y, z));
            if (ex * ex + ey * ey + ez * ez <= r * r) {
                return Hash.hash01(leafSeed, x, y, z) < blossomShare ? EndBlock.GROVE_BLOSSOM : EndBlock.GROVE_LEAVES;
            }
        }
        return null;
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
