package chlorine.etjourney.world.end.feature.plant;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import chlorine.etjourney.world.end.feature.sdf.Sdf;
import chlorine.etjourney.world.end.feature.sdf.SdfFill;
import chlorine.etjourney.world.end.feature.sdf.Spline;
import chlorine.etjourney.world.end.modifier.EndBlock;

/** AMBER's seed tree: three strands wind up round a glowing drop from the ground, join above it, and bear fruits. */
public final class AmberTree implements PlantCells.Bounds {

    /** Blocks the legs reach below their foot, so the tree does not float on a slope. */
    public static final int SINK = 4;

    /** Ground further below the root than this counts as none, such as a lake or the void. */
    private static final int FLOOR = -2 * SINK;
    private static final int FIT_TRIES = 6;
    private static final int STRAND_POINTS = 10;
    private static final int REACH = 12;
    /** How deep a strand's centre line sinks into the seed, as a share of its thickness. */
    private static final double HUG_SINK = 0.4;
    /** The seed's height to its width. */
    private static final double FLAT = 0.6;
    /** How much taller the lower half of the seed is than the upper. */
    private static final double LOW_STRETCH = 1.15;
    /** How much wider the seed swells towards its bottom. */
    private static final double BULGE = 0.4;
    /** How far a diving leg opens out per block it goes down. */
    private static final double FLARE = 0.3;
    private static final int[][] FACES = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 },
        { 0, 0, -1 } };

    /** The ground as the tree sees it. */
    public interface Ground {

        /** Height of the topmost ground block at (dx, dz) from the root, relative to the root's y (flat: -1). */
        int top(int dx, int dz);
    }

    public interface CellVisitor {

        void cell(int dx, int dy, int dz, EndBlock block);
    }

    private enum Part {
        LEG,
        TRUNK,
        BRANCH,
        SEED
    }

    private final int[] xs, ys, zs;
    private final EndBlock[] blocks;
    private final int strands;
    private final int minX, maxX, minY, maxY, minZ, maxZ;

    private AmberTree(Map<Long, Part> cells, int strands) {
        int n = cells.size();
        xs = new int[n];
        ys = new int[n];
        zs = new int[n];
        blocks = new EndBlock[n];
        int i = 0, x0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, y0 = x0, y1 = x1, z0 = x0, z1 = x1;
        for (boolean seedPass : new boolean[] { false, true }) {
            for (Map.Entry<Long, Part> e : cells.entrySet()) {
                if ((e.getValue() == Part.SEED) != seedPass) continue;
                long k = e.getKey();
                xs[i] = SdfFill.x(k);
                ys[i] = SdfFill.y(k);
                zs[i] = SdfFill.z(k);
                blocks[i] = seedPass ? EndBlock.AMBER_SEED : EndBlock.AMBER_ROOT;
                x0 = Math.min(x0, xs[i]);
                x1 = Math.max(x1, xs[i]);
                y0 = Math.min(y0, ys[i]);
                y1 = Math.max(y1, ys[i]);
                z0 = Math.min(z0, zs[i]);
                z1 = Math.max(z1, zs[i]);
                i++;
            }
        }
        minX = x0;
        maxX = x1;
        minY = y0;
        maxY = y1;
        minZ = z0;
        maxZ = z1;
        this.strands = strands;
    }

    /** Builds the tree; it shrinks to fit under {@code maxTop}, and is null when it still does not fit. */
    public static AmberTree of(long shapeSeed, int maxTop, Ground ground) {
        double scale = 1;
        for (int i = 0; i <= FIT_TRIES; i++, scale *= 0.9) {
            AmberTree t = build(shapeSeed, scale, ground);
            if (t.maxY <= maxTop) return t;
        }
        return null;
    }

    /** Whether the block is the tree's wood, standing or lying. */
    public static boolean wood(EndBlock block) {
        return block == EndBlock.AMBER_ROOT || block == EndBlock.AMBER_ROOT_X || block == EndBlock.AMBER_ROOT_Z;
    }

    private static AmberTree build(long shapeSeed, double scale, Ground ground) {
        Random r = new Random(shapeSeed);

        // The seed: a squat ball with a tapering tip leaning off to one side. Its lower half swells out and sinks
        // in, so it rests on the ground to its rim.
        double radius = (7 + r.nextDouble() * 0.8) * scale;
        double half = radius * FLAT;
        double cy = half - 0.5 - 0.9 * scale;
        double leanAngle = r.nextDouble() * Math.PI * 2;
        double lean = (0.8 + r.nextDouble()) * scale;
        double lx = Math.cos(leanAngle) * lean, lz = Math.sin(leanAngle) * lean;
        double bodyTop = cy + half;
        double tipY = bodyTop + 2.2 * scale;
        Sdf body = (x, y, z) -> {
            double dy = y - cy;
            if (dy >= 0) return Math.sqrt(x * x + sq(dy / FLAT) + z * z) - radius;
            double ry = FLAT * LOW_STRETCH;
            double f = Math.min(1, -dy / (radius * ry));
            double s = 1 + BULGE * Math.sin(f * Math.PI / 2);
            return Math.sqrt(sq(x / s) + sq(dy / ry) + sq(z / s)) - radius;
        };
        Sdf seed = body.smoothUnion(
            Sdf.line(lx * 0.3, cy, lz * 0.3, lx * 0.7, bodyTop - 0.3, lz * 0.7, radius * 0.55)
                .union(Sdf.line(lx * 0.7, bodyTop - 0.3, lz * 0.7, lx, tipY, lz, 0.8 * scale)),
            2)
            .displace(LucentShapes.noise(r.nextLong(), 0.3));

        // The legs: three thick strands winding up round the seed from the ground, closing above it.
        r.nextDouble(); // once picked two or three strands; still drawn so each seed keeps its shape
        int strands = 3;
        double merge = tipY + (2.8 + r.nextDouble() * 1.5) * scale;
        // Three strands wind less than two would, so the seed still shows between them.
        double twist = (9 + r.nextDouble() * 1.5) * (r.nextBoolean() ? 1 : -1) * 0.6;
        double start = r.nextDouble() * Math.PI * 2;
        double thick = 2, thin = 0.55;
        Sdf legs = null;
        List<List<double[]>> paths = new ArrayList<>();
        // Every wooden part's centre line, legs first.
        List<List<double[]>> lines = new ArrayList<>();
        double lowest = 0;
        for (int s = 0; s < strands; s++) {
            double a0 = start + (s + (r.nextDouble() - 0.5) * 0.3) * Math.PI * 2 / strands;
            double turn = twist * (0.85 + r.nextDouble() * 0.3);
            double phase = r.nextDouble() * Math.PI * 2;
            List<double[]> path = new ArrayList<>();
            for (int k = 0; k < STRAND_POINTS; k++) {
                double t = (double) k / (STRAND_POINTS - 1);
                double y = t * merge;
                double a = a0 + turn * t + 0.2 * Math.sin(phase + 5 * t);
                double follow = Math.max(0, Math.min(1, (y - cy) / (tipY - cy)));
                double ax = lx * follow, az = lz * follow;
                double sink = -thick * HUG_SINK;
                double d;
                if (y <= tipY) {
                    // Beside the seed the strand sinks into it, down its skirt to the ground.
                    d = clearance(seed, ax, Math.max(y, 0.5), az, a, sink);
                } else {
                    // Above the tip it closes in on the trunk from where it left the seed.
                    double atTip = clearance(seed, lx, tipY, lz, a, sink);
                    d = atTip + (thin - atTip) * (y - tipY) / (merge - tipY);
                }
                path.add(new double[] { ax + Math.cos(a) * d, y, az + Math.sin(a) * d });
            }
            double[] foot = path.get(0);
            List<double[]> full = path;
            int under = ground.top((int) Math.floor(foot[0]), (int) Math.floor(foot[2]));
            if (under >= FLOOR) {
                double bottom = Math.min(0, under + 1) - SINK;
                // Below its foot the leg winds on into the ground, opening out as it goes: that is its root.
                double d0 = Math.hypot(foot[0], foot[2]);
                List<double[]> dive = new ArrayList<>();
                for (int j = 1;; j++) {
                    double t = (double) -j / (STRAND_POINTS - 1);
                    double y = Math.max(t * merge, bottom);
                    double a = a0 + turn * t + 0.2 * Math.sin(phase + 5 * t);
                    double d = d0 + FLARE * -y;
                    double x = Math.cos(a) * d, z = Math.sin(a) * d;
                    // A root stops where the ground does, short of a lake or the void.
                    if (ground.top((int) Math.floor(x), (int) Math.floor(z)) < FLOOR) break;
                    dive.add(0, new double[] { x, y, z });
                    lowest = Math.min(lowest, y);
                    if (y <= bottom) break;
                }
                if (dive.isEmpty()) path.add(0, foot.clone());
                else {
                    full = new ArrayList<>(dive);
                    full.addAll(path);
                    path.add(0, dive.get(0));
                }
            } else {
                // No ground to stand on: the leg ends at its foot.
                path.add(0, foot.clone());
            }
            paths.add(path);
            lines.add(full);
            Sdf strand = Spline.shape(full, thick, thin);
            legs = legs == null ? strand : legs.union(strand);
        }

        // The trunk above the knot.
        double topY = (20 + r.nextDouble() * 3) * scale;
        List<double[]> trunk = Spline.make(lx, merge, lz, lx, topY, lz, 5);
        Spline.jitter(trunk, r, 1.85, 0, 1.85, 2);
        lines.add(trunk);
        Sdf trunkShape = Spline.shape(trunk, 1.3, 0.75);
        Sdf wood = legs.smoothUnion(trunkShape, 2);

        Map<Long, Part> cells = new LinkedHashMap<>();
        int top = (int) Math.ceil(topY + 2);
        // A leg's end cap reaches about one thickness under its bottom point.
        int bottom = (int) Math.floor(lowest) - 2;
        for (int x = -REACH; x <= REACH; x++) {
            for (int z = -REACH; z <= REACH; z++) {
                for (int y = bottom; y <= top; y++) {
                    // Sample at the block's centre, where line() puts its points too.
                    double px = x + 0.5, py = y + 0.5, pz = z + 0.5;
                    if (wood.distance(px, py, pz) < 0) {
                        boolean inTrunk = trunkShape.distance(px, py, pz) < legs.distance(px, py, pz);
                        cells.put(SdfFill.key(x, y, z), inTrunk ? Part.TRUNK : Part.LEG);
                    } else if (seed.distance(px, py, pz) < 0) cells.put(SdfFill.key(x, y, z), Part.SEED);
                }
            }
        }
        // The centre lines keep the thin strands face-connected.
        for (int s = 0; s < strands; s++) BlockLines.line(lines.get(s), cells, Part.LEG);
        BlockLines.line(trunk, cells, Part.TRUNK);

        // Branches out of the upper strands, each with a fruit at its end.
        int branches = 6 + r.nextInt(3);
        for (int b = 0; b < branches; b++) {
            List<double[]> path = paths.get(r.nextInt(strands));
            double t = 0.65 + r.nextDouble() * 0.3;
            double[] from = along(path.subList(1, path.size()), t);
            double ox = from[0] - lx, oz = from[2] - lz, len = Math.hypot(ox, oz);
            if (len < 0.5) {
                double a = r.nextDouble() * Math.PI * 2;
                ox = Math.cos(a);
                oz = Math.sin(a);
                len = 1;
            }
            double reach = (4 + r.nextDouble() * 2) * scale;
            double size = (0.5 + r.nextDouble() * 0.6) * Math.max(0.7, scale);
            double out = Math.hypot(from[0] + ox / len * reach, from[2] + oz / len * reach);
            if (out + size > 9.5) reach = Math.max(1.5, reach - (out + size - 9.5));
            double[] tip = { from[0] + ox / len * reach, from[1] + reach * 0.9, from[2] + oz / len * reach };
            // The branch ends just inside the fruit, so the fruit hangs on it but stays one piece.
            double stop = Math.max(0, (reach - size * 0.5) / reach);
            List<double[]> branch = new ArrayList<>();
            branch.add(from);
            branch.add(
                new double[] { from[0] + (tip[0] - from[0]) * stop, from[1] + (tip[1] - from[1]) * stop,
                    from[2] + (tip[2] - from[2]) * stop });
            BlockLines.line(branch, cells, Part.BRANCH);
            List<double[]> whole = new ArrayList<>();
            whole.add(from);
            whole.add(tip);
            lines.add(whole);
            if (!fruit(tip, size, cells)) {
                // Rounding left a gap: run the branch on into the fruit's centre.
                branch.set(1, tip);
                BlockLines.line(branch, cells, Part.BRANCH);
            }
        }
        return new AmberTree(cells, strands);
    }

    private static double sq(double v) {
        return v * v;
    }

    /** How far out along angle {@code a} from (ax, y, az) a strand of the given clearance clears the seed. */
    private static double clearance(Sdf seed, double ax, double y, double az, double a, double clear) {
        double c = Math.cos(a), s = Math.sin(a);
        double d = 0;
        for (; d < REACH; d += 0.25) if (seed.distance(ax + c * d, y, az + s * d) > clear) break;
        return d;
    }

    /** The point a fraction {@code t} of the way along the polyline, by point index. */
    private static double[] along(List<double[]> path, double t) {
        double f = t * (path.size() - 1);
        int i = Math.min(path.size() - 2, (int) Math.floor(f));
        double u = f - i;
        double[] a = path.get(i), b = path.get(i + 1);
        return new double[] { a[0] + (b[0] - a[0]) * u, a[1] + (b[1] - a[1]) * u, a[2] + (b[2] - a[2]) * u };
    }

    /** A ball of seed around the tip, leaving wood as it is; whether it touches wood. */
    private static boolean fruit(double[] tip, double radius, Map<Long, Part> cells) {
        int cx = (int) Math.floor(tip[0]), cy = (int) Math.floor(tip[1]), cz = (int) Math.floor(tip[2]);
        int reach = (int) Math.ceil(radius);
        List<Long> placed = new ArrayList<>();
        for (int x = -reach; x <= reach; x++) {
            for (int y = -reach; y <= reach; y++) {
                for (int z = -reach; z <= reach; z++) {
                    if (x * x + y * y + z * z > radius * radius + 0.5) continue;
                    long k = SdfFill.key(cx + x, cy + y, cz + z);
                    if (isWood(cells.get(k))) continue;
                    cells.put(k, Part.SEED);
                    placed.add(k);
                }
            }
        }
        for (long k : placed)
            for (int[] f : FACES) if (isWood(cells.get(SdfFill.offset(k, f[0], f[1], f[2])))) return true;
        return false;
    }

    private static boolean isWood(Part p) {
        return p != null && p != Part.SEED;
    }

    /** Wood first, then seed and fruit. */
    public void forEach(CellVisitor v) {
        for (int i = 0; i < blocks.length; i++) v.cell(xs[i], ys[i], zs[i], blocks[i]);
    }

    public int strands() {
        return strands;
    }

    @Override
    public int minX() {
        return minX;
    }

    @Override
    public int maxX() {
        return maxX;
    }

    @Override
    public int minY() {
        return minY;
    }

    @Override
    public int maxY() {
        return maxY;
    }

    @Override
    public int minZ() {
        return minZ;
    }

    @Override
    public int maxZ() {
        return maxZ;
    }
}
