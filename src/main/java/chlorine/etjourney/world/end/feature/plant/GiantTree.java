package chlorine.etjourney.world.end.feature.plant;

import java.util.Random;

import chlorine.etjourney.world.end.modifier.BlockSink;
import chlorine.etjourney.world.end.modifier.EndBlock;
import chlorine.etjourney.world.end.noise.Hash;

/**
 * One giant tree, shaped from the world seed and its root alone so every chunk draws the same tree: a hollow ribbed
 * trunk about 20 across that flares at the foot, crawling roots, and a few branches ending in clumps of leaves.
 */
public final class GiantTree {

    /** How far below the root's ground the trunk carries on, overwriting, to meet ground lower than the probe's. */
    static final int DEPTH = 8;
    private static final int FLARE_HEIGHT = 8;
    private static final double FLARE_RADIUS = 13;
    private static final double SHELL = 3;
    private static final double RIB = 0.7;
    private static final int TAPER = 12;
    private static final double TOP_RADIUS = 5;
    private static final double LEAF_SQUASH = 0.75;
    private static final long SALT = 0x5DEECE66D3A1F2B7L;
    private static final long LEAF_SALT = 0x3C6EF372FE94F82BL;

    private final double ax, az;
    private final int base, top;
    private final double radius;
    private final int lobes, fineLobes;
    private final double phase, finePhase;
    private final double blossomShare;
    private final long leafSeed;
    /** Tubes as {x0, y0, z0, r0, x1, y1, z1, r1}: roots, then branches. */
    private final double[][] tubes;
    private final int roots;
    /** Leaf clumps as {x, y, z, radius}. */
    private final double[][] clumps;

    public GiantTree(long seed, double[] anchor) {
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

        roots = 6 + random.nextInt(4);
        int branches = 4 + random.nextInt(3);
        tubes = new double[roots * 5 + branches][];
        double start = random.nextDouble() * 2 * Math.PI;
        for (int i = 0; i < roots; i++) {
            double angle = start + (i + (random.nextDouble() - 0.5) * 0.5) * 2 * Math.PI / roots;
            double length = 18 + random.nextDouble() * 5, curve = (random.nextDouble() - 0.5) * 0.7;
            double sink = 1 + random.nextDouble() * 1.5;
            double[] previous = null;
            for (int k = 0; k <= 5; k++) {
                double t = k / 5.0, h = 7 + (length - 7) * t, a = angle + curve * t * t;
                double[] point = { ax + h * Math.cos(a), base + 5 - (4.5 + sink) * Math.pow(t, 1.5),
                    az + h * Math.sin(a), 3 - 1.2 * t };
                if (previous != null) tubes[i * 5 + k - 1] = join(previous, point);
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
            tubes[roots * 5 + i] = new double[] { ax + 6 * Math.cos(angle), from, az + 6 * Math.sin(angle), 2, ex, to,
                ez, 1.4 };
            clumps[i] = clump(ex, to + 2, ez, leaf);
        }
        clumps[branches] = clump(ax, base + top + 1, az, 7 + random.nextDouble() * 1.5);
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
        int y0 = Math.max(sink.minY(), base - DEPTH), y1 = Math.min(sink.maxY() - 1, base + GiantTrees.HEIGHT - 1);
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                double dx = x + 0.5 - ax, dz = z + 0.5 - az, d = Math.sqrt(dx * dx + dz * dz);
                if (d > reach) continue;
                double angle = Math.atan2(dz, dx);
                for (int y = y0; y <= y1; y++) {
                    EndBlock b = block(x, y, z, d, angle);
                    if (b == null) continue;
                    if (b == EndBlock.GROVE_LOG && y < base) sink.set(x, y, z, b);
                    else sink.place(x, y, z, b);
                }
            }
        }
    }

    private EndBlock block(int x, int y, int z, double d, double angle) {
        int dy = y - base;
        if (dy >= -DEPTH && dy <= top && d <= trunk(dy, angle)) {
            double inner = Double.MAX_VALUE;
            for (int k = -2; k <= 2; k++) inner = Math.min(inner, trunk(Math.max(-DEPTH, dy + k), angle));
            return d > inner - SHELL ? EndBlock.GROVE_LOG : null;
        }
        double px = x + 0.5, py = y + 0.5, pz = z + 0.5;
        int first = dy <= 9 ? 0 : roots * 5, last = dy >= top - 20 ? tubes.length : roots * 5;
        for (int i = first; i < last; i++) if (inTube(tubes[i], px, py, pz)) return EndBlock.GROVE_LOG;
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

    private static boolean inTube(double[] t, double px, double py, double pz) {
        double vx = t[4] - t[0], vy = t[5] - t[1], vz = t[6] - t[2];
        double wx = px - t[0], wy = py - t[1], wz = pz - t[2];
        double along = Math.max(0, Math.min(1, (wx * vx + wy * vy + wz * vz) / (vx * vx + vy * vy + vz * vz)));
        double r = t[3] + (t[7] - t[3]) * along;
        double ox = wx - vx * along, oy = wy - vy * along, oz = wz - vz * along;
        return ox * ox + oy * oy + oz * oz <= r * r;
    }
}
