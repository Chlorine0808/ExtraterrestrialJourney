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

/** AMBER's seed tree: twisted strands hug a glowing drop, join into one trunk above it, and bear a few fruits. */
public final class AmberTree {

    /** Blocks the legs reach below their foot, so the tree does not float on a slope. */
    public static final int SINK = 4;

    /** Ground further below the root than this counts as none, such as a lake or the void. */
    private static final int FLOOR = -2 * SINK;
    private static final int FIT_TRIES = 6;
    private static final int STRAND_POINTS = 10;
    private static final int REACH = 12;
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

    private final int[] xs, ys, zs;
    private final EndBlock[] blocks;
    private final int strands;
    private final int minX, maxX, minY, maxY, minZ, maxZ;

    private AmberTree(Map<Long, EndBlock> cells, int strands) {
        int n = cells.size();
        xs = new int[n];
        ys = new int[n];
        zs = new int[n];
        blocks = new EndBlock[n];
        int i = 0, x0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, y0 = x0, y1 = x1, z0 = x0, z1 = x1;
        for (EndBlock kind : new EndBlock[] { EndBlock.AMBER_ROOT, EndBlock.AMBER_SEED }) {
            for (Map.Entry<Long, EndBlock> e : cells.entrySet()) {
                if (e.getValue() != kind) continue;
                long k = e.getKey();
                xs[i] = SdfFill.x(k);
                ys[i] = SdfFill.y(k);
                zs[i] = SdfFill.z(k);
                blocks[i] = kind;
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

    private static AmberTree build(long shapeSeed, double scale, Ground ground) {
        Random r = new Random(shapeSeed);

        // The seed: a squat ball with a tapering tip leaning off to one side.
        double radius = (3.6 + r.nextDouble() * 0.8) * scale;
        double half = radius * 0.55;
        double cy = half - 0.5;
        double leanAngle = r.nextDouble() * Math.PI * 2;
        double lean = (0.8 + r.nextDouble()) * scale;
        double lx = Math.cos(leanAngle) * lean, lz = Math.sin(leanAngle) * lean;
        double bodyTop = cy + half;
        double tipY = bodyTop + 1.2 * scale;
        Sdf seed = Sdf.sphere(radius)
            .scale(1, 0.55, 1)
            .translate(0, cy, 0)
            .smoothUnion(
                Sdf.line(lx * 0.3, cy, lz * 0.3, lx * 0.7, bodyTop - 0.3, lz * 0.7, radius * 0.55)
                    .union(Sdf.line(lx * 0.7, bodyTop - 0.3, lz * 0.7, lx, tipY, lz, 0.8 * scale)),
                2)
            .displace(LucentShapes.noise(r.nextLong(), 0.5));

        // The strands: an uneven helix that spreads into legs, hugs the seed, and closes above it.
        double pick = r.nextDouble();
        int strands = pick < 0.45 ? 4 : pick < 0.85 ? 5 : 6;
        double merge = tipY + (2.5 + r.nextDouble() * 1.5) * scale;
        double twist = (0.6 + r.nextDouble() * 0.6) * (r.nextBoolean() ? 1 : -1);
        double start = r.nextDouble() * Math.PI * 2;
        double legs = radius + 2.2 * scale;
        double thick = 0.95, thin = 0.8;
        Sdf wood = null;
        List<List<double[]>> paths = new ArrayList<>();
        double lowest = 0;
        for (int s = 0; s < strands; s++) {
            double a0 = start + (s + (r.nextDouble() - 0.5) * 0.5) * Math.PI * 2 / strands;
            double turn = twist * (0.7 + r.nextDouble() * 0.6);
            double phase = r.nextDouble() * Math.PI * 2;
            List<double[]> path = new ArrayList<>();
            for (int k = 0; k < STRAND_POINTS; k++) {
                double t = (double) k / (STRAND_POINTS - 1);
                double y = t * merge;
                double a = a0 + turn * t + 0.35 * Math.sin(phase + 5 * t);
                double follow = Math.max(0, Math.min(1, (y - cy) / (tipY - cy)));
                double ax = lx * follow, az = lz * follow;
                double taper = legs + (thin - legs) * t;
                double hug = t >= 1 ? 0 : clearance(seed, ax, y, az, a, thick + 0.3);
                double d = Math.max(taper, hug);
                path.add(new double[] { ax + Math.cos(a) * d, y, az + Math.sin(a) * d });
            }
            double[] foot = path.get(0);
            int fx = (int) Math.floor(foot[0]), fz = (int) Math.floor(foot[2]);
            int under = ground.top(fx, fz);
            if (under >= FLOOR) {
                double bottom = Math.min(0, under + 1) - SINK;
                path.add(0, new double[] { foot[0], bottom, foot[2] });
                lowest = Math.min(lowest, bottom);
            } else {
                // No ground to stand on: the leg ends at its foot, and the root step below starts there.
                path.add(0, foot.clone());
            }
            paths.add(path);
            Sdf strand = Spline.shape(path, thick, thin);
            wood = wood == null ? strand : wood.union(strand);
        }

        // The trunk above the knot.
        double topY = (13 + r.nextDouble() * 3) * scale;
        List<double[]> trunk = Spline.make(lx, merge, lz, lx, topY, lz, 5);
        Spline.jitter(trunk, r, 0.6, 0, 0.6, 2);
        wood = wood.smoothUnion(Spline.shape(trunk, 1.3, 0.9), 2);

        Map<Long, EndBlock> cells = new LinkedHashMap<>();
        int top = (int) Math.ceil(topY + 2);
        // A leg's end cap reaches about one thickness under its bottom point.
        int bottom = (int) Math.floor(lowest) - 2;
        for (int x = -REACH; x <= REACH; x++) {
            for (int z = -REACH; z <= REACH; z++) {
                for (int y = bottom; y <= top; y++) {
                    // Sample at the block's centre, where line() puts its points too.
                    double px = x + 0.5, py = y + 0.5, pz = z + 0.5;
                    if (wood.distance(px, py, pz) < 0) cells.put(SdfFill.key(x, y, z), EndBlock.AMBER_ROOT);
                    else if (seed.distance(px, py, pz) < 0) cells.put(SdfFill.key(x, y, z), EndBlock.AMBER_SEED);
                }
            }
        }
        // The centre lines keep the thin strands face-connected.
        for (List<double[]> path : paths) line(path, cells);
        line(trunk, cells);

        // Branches out of the upper strands, each with a fruit at its end.
        int branches = 2 + r.nextInt(3);
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
            double reach = (3 + r.nextDouble() * 2) * scale;
            double size = (1.0 + r.nextDouble() * 0.6) * Math.max(0.7, scale);
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
            line(branch, cells);
            if (!fruit(tip, size, cells)) {
                // Rounding left a gap: run the branch on into the fruit's centre.
                branch.set(1, tip);
                line(branch, cells);
            }
        }

        // Roots crawling out from each leg over the ground.
        for (List<double[]> path : paths) {
            double[] foot = path.get(1);
            double a = Math.atan2(foot[2], foot[0]);
            int count = 1 + r.nextInt(2);
            for (int k = 0; k < count; k++) {
                double angle = a + (r.nextDouble() - 0.5) * 0.8;
                double from = Math.hypot(foot[0], foot[2]);
                double to = Math.min(9.8, from + (2.5 + r.nextDouble() * 1.5) * scale);
                List<double[]> root = new ArrayList<>();
                root.add(new double[] { foot[0], 0, foot[2] });
                for (int p = 1; p < 4; p++) {
                    double d = from + (to - from) * p / 3;
                    double x = Math.cos(angle) * d, z = Math.sin(angle) * d;
                    int under = ground.top((int) Math.floor(x), (int) Math.floor(z));
                    // A root crawls over the ground only, so it stops where the ground ends.
                    if (under < FLOOR) break;
                    root.add(new double[] { x, under + 1, z });
                }
                if (root.size() > 1) line(root, cells);
            }
        }
        return new AmberTree(cells, strands);
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

    /** Wood along the polyline, one block thick and face-connected. */
    private static void line(List<double[]> spline, Map<Long, EndBlock> cells) {
        int[] prev = null;
        for (int i = 0; i + 1 < spline.size(); i++) {
            double[] a = spline.get(i), b = spline.get(i + 1);
            double len = Math.sqrt(sq(b[0] - a[0]) + sq(b[1] - a[1]) + sq(b[2] - a[2]));
            int steps = Math.max(1, (int) Math.ceil(len * 3));
            for (int s = 0; s <= steps; s++) {
                double t = (double) s / steps;
                int[] cell = { (int) Math.floor(a[0] + (b[0] - a[0]) * t), (int) Math.floor(a[1] + (b[1] - a[1]) * t),
                    (int) Math.floor(a[2] + (b[2] - a[2]) * t) };
                if (prev != null) {
                    for (int axis = 0; axis < 3; axis++) {
                        if (prev[axis] == cell[axis]) continue;
                        prev[axis] = cell[axis];
                        cells.put(SdfFill.key(prev[0], prev[1], prev[2]), EndBlock.AMBER_ROOT);
                    }
                } else {
                    prev = cell.clone();
                }
                cells.put(SdfFill.key(cell[0], cell[1], cell[2]), EndBlock.AMBER_ROOT);
            }
        }
    }

    /** A ball of seed around the tip, leaving wood as it is; whether it touches wood. */
    private static boolean fruit(double[] tip, double radius, Map<Long, EndBlock> cells) {
        int cx = (int) Math.floor(tip[0]), cy = (int) Math.floor(tip[1]), cz = (int) Math.floor(tip[2]);
        int reach = (int) Math.ceil(radius);
        List<Long> placed = new ArrayList<>();
        for (int x = -reach; x <= reach; x++) {
            for (int y = -reach; y <= reach; y++) {
                for (int z = -reach; z <= reach; z++) {
                    if (x * x + y * y + z * z > radius * radius + 0.5) continue;
                    long k = SdfFill.key(cx + x, cy + y, cz + z);
                    if (cells.get(k) == EndBlock.AMBER_ROOT) continue;
                    cells.put(k, EndBlock.AMBER_SEED);
                    placed.add(k);
                }
            }
        }
        for (long k : placed) for (int[] f : FACES)
            if (cells.get(SdfFill.offset(k, f[0], f[1], f[2])) == EndBlock.AMBER_ROOT) return true;
        return false;
    }

    private static double sq(double v) {
        return v * v;
    }

    /** Wood first, then seed and fruit. */
    public void forEach(CellVisitor v) {
        for (int i = 0; i < blocks.length; i++) v.cell(xs[i], ys[i], zs[i], blocks[i]);
    }

    public int strands() {
        return strands;
    }

    public int minX() {
        return minX;
    }

    public int maxX() {
        return maxX;
    }

    public int minY() {
        return minY;
    }

    public int maxY() {
        return maxY;
    }

    public int minZ() {
        return minZ;
    }

    public int maxZ() {
        return maxZ;
    }
}
