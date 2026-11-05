package chlorine.etjourney.world.end.feature.plant;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import chlorine.etjourney.world.end.feature.sdf.Sdf;
import chlorine.etjourney.world.end.feature.sdf.SdfFill;
import chlorine.etjourney.world.end.modifier.EndBlock;

/** BetterEnd's lucernia at full size: branches fanning out of one foot, each ending in a leaf ball hung with vines. */
public final class LucentTree {

    /** Blocks the foot reaches below the root, so the tree does not float where the probe's ground runs high. */
    public static final int SINK = 6;
    /** Share of the hanging points that carry a lantern instead of a long vine. */
    static final double LANTERN_SHARE = 0.3;

    private static final double MIN_SIZE = 6;
    private static final int FIT_TRIES = 12;
    private static final double[][] BRANCH = { { 0, 0, 0 }, { 0.10, 0.35, 0 }, { 0.20, 0.50, 0 }, { 0.30, 0.55, 0 },
        { 0.42, 0.70, 0 }, { 0.50, 1.00, 0 } };
    private static final double[][] ROOT = { { 0.1, 0.25, 0 }, { 0.3, -0.15, 0 }, { 0.7, -0.40, 0 },
        { 0.8, -0.65, 0 } };
    private static final int[][] FACES = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 },
        { 0, 0, -1 } };
    private static final int[][] FUR_FACES = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };

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
    private final double size;
    private final int branches;
    private final int minX, maxX, minY, maxY, minZ, maxZ;

    private LucentTree(List<int[]> cells, List<EndBlock> kinds, double size, int branches) {
        int n = cells.size();
        xs = new int[n];
        ys = new int[n];
        zs = new int[n];
        blocks = kinds.toArray(new EndBlock[0]);
        int x0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, y0 = x0, y1 = x1, z0 = x0, z1 = x1;
        for (int i = 0; i < n; i++) {
            int[] c = cells.get(i);
            xs[i] = c[0];
            ys[i] = c[1];
            zs[i] = c[2];
            x0 = Math.min(x0, c[0]);
            x1 = Math.max(x1, c[0]);
            y0 = Math.min(y0, c[1]);
            y1 = Math.max(y1, c[1]);
            z0 = Math.min(z0, c[2]);
            z1 = Math.max(z1, c[2]);
        }
        minX = x0;
        maxX = x1;
        minY = y0;
        maxY = y1;
        minZ = z0;
        maxZ = z1;
        this.size = size;
        this.branches = branches;
    }

    /** Builds the tree; it shrinks to fit under {@code maxTop}, and is null when it would drop below the minimum. */
    public static LucentTree of(long shapeSeed, double size, int maxTop, Ground ground) {
        for (int i = 0; i <= FIT_TRIES && size >= MIN_SIZE; i++, size *= 0.9) {
            LucentTree t = build(shapeSeed, size, ground);
            if (t.maxY <= maxTop) return t;
        }
        return null;
    }

    private static LucentTree build(long shapeSeed, double size, Ground ground) {
        Random r = new Random(shapeSeed);
        Map<Long, EndBlock> solid = new HashMap<>();
        int count = (int) (size * 0.3);
        double var = Math.PI * 2 / (count * 3);
        double start = r.nextDouble() * Math.PI * 2;
        List<double[]> tips = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double angle = (double) i / count * Math.PI * 2 + r.nextDouble() * var + start;
            List<double[]> spline = posed(BRANCH, angle, size * (0.5 + r.nextDouble() * 0.5));
            for (int k = 1; k < spline.size(); k++) {
                spline.get(k)[0] += r.nextDouble() * 2 - 1;
                spline.get(k)[2] += r.nextDouble() * 2 - 1;
            }
            BlockLines.line(spline, solid, EndBlock.LUCENT_LOG);
            tips.add(spline.get(spline.size() - 1));
        }

        int rootStart = 3 + r.nextInt(3);
        for (int y = -SINK; y <= rootStart; y++) solid.put(SdfFill.key(0, y, 0), EndBlock.LUCENT_LOG);

        List<Long> hangs = new ArrayList<>();
        for (double[] tip : tips) ball(r, tip, size, solid, hangs);

        double rootRadius = size * 0.35;
        int roots = (int) (rootRadius * 1.5);
        for (int i = 0; i < roots; i++) {
            double angle = (double) i / roots * Math.PI * 2;
            List<double[]> spline = posed(ROOT, angle, rootRadius * (0.85 + r.nextDouble() * 0.3));
            for (double[] p : spline) p[1] += rootStart;
            double[] tip = spline.get(spline.size() - 1);
            int tx = (int) Math.floor(tip[0]), tz = (int) Math.floor(tip[2]);
            // Upstream keeps a root only when its tip ends in end stone.
            if (Math.floor(tip[1]) <= ground.top(tx, tz)) BlockLines.line(spline, solid, EndBlock.LUCENT_LOG);
        }

        pruneLeaves(solid);

        List<int[]> cells = new ArrayList<>();
        List<EndBlock> kinds = new ArrayList<>();
        for (EndBlock kind : new EndBlock[] { EndBlock.LUCENT_LOG, EndBlock.LUCENT_LEAVES }) {
            for (Map.Entry<Long, EndBlock> e : solid.entrySet()) {
                if (e.getValue() != kind) continue;
                long k = e.getKey();
                cells.add(new int[] { SdfFill.x(k), SdfFill.y(k), SdfFill.z(k) });
                kinds.add(kind);
            }
        }
        Map<Long, EndBlock> hanging = runs(r, hangs, solid);
        Set<Long> fur = new LinkedHashSet<>();
        for (Map.Entry<Long, EndBlock> e : solid.entrySet()) {
            if (e.getValue() != EndBlock.LUCENT_LEAVES) continue;
            long k = e.getKey();
            for (int[] f : FUR_FACES) {
                long n = SdfFill.offset(k, f[0], f[1], f[2]);
                if (solid.containsKey(n) || hanging.containsKey(n)) continue;
                // Fur never hangs under a leaf; that side is left to the vines.
                if (solid.get(SdfFill.offset(n, 0, 1, 0)) == EndBlock.LUCENT_LEAVES) continue;
                fur.add(n);
            }
        }
        for (long n : fur) {
            cells.add(new int[] { SdfFill.x(n), SdfFill.y(n), SdfFill.z(n) });
            kinds.add(EndBlock.LUCENT_FUR);
        }
        for (Map.Entry<Long, EndBlock> e : hanging.entrySet()) {
            long k = e.getKey();
            cells.add(new int[] { SdfFill.x(k), SdfFill.y(k), SdfFill.z(k) });
            kinds.add(e.getValue());
        }
        return new LucentTree(cells, kinds, size, count);
    }

    /** Drops the leaves farther from wood, walking through leaves, than they may stand without decaying. */
    private static void pruneLeaves(Map<Long, EndBlock> solid) {
        Map<Long, Integer> steps = new HashMap<>();
        ArrayDeque<Long> open = new ArrayDeque<>();
        for (Map.Entry<Long, EndBlock> e : solid.entrySet()) {
            if (e.getValue() != EndBlock.LUCENT_LOG) continue;
            steps.put(e.getKey(), 0);
            open.add(e.getKey());
        }
        while (!open.isEmpty()) {
            long c = open.poll();
            int next = steps.get(c) + 1;
            if (next > Grower.LEAF_REACH) continue;
            for (int[] f : FACES) {
                long n = SdfFill.offset(c, f[0], f[1], f[2]);
                if (solid.get(n) != EndBlock.LUCENT_LEAVES || steps.containsKey(n)) continue;
                steps.put(n, next);
                open.add(n);
            }
        }
        solid.entrySet()
            .removeIf(e -> e.getValue() == EndBlock.LUCENT_LEAVES && !steps.containsKey(e.getKey()));
    }

    /** The template points turned by {@code angle} about the vertical axis and scaled. */
    private static List<double[]> posed(double[][] template, double angle, double scale) {
        double c = Math.cos(angle), s = Math.sin(angle);
        List<double[]> out = new ArrayList<>(template.length);
        for (double[] p : template)
            out.add(new double[] { (p[0] * c - p[2] * s) * scale, p[1] * scale, (p[0] * s + p[2] * c) * scale });
        return out;
    }

    /** One leaf ball around a branch tip, as upstream's leavesBall; hanging points go to {@code hangs}. */
    private static void ball(Random r, double[] tip, double size, Map<Long, EndBlock> solid, List<Long> hangs) {
        int cx = (int) Math.floor(tip[0]), cy = (int) Math.floor(tip[1]), cz = (int) Math.floor(tip[2]);
        double radius = (size * 0.13 + 0.8 + r.nextDouble() * 0.7) * 1.4;
        long noiseSeed = r.nextLong(), jitterSeed = r.nextLong();
        Sdf sphere = Sdf.sphere(radius);
        Sdf shape = sphere.subtract(
            sphere.scale(5)
                .translate(0, -radius * 5, 0))
            .scale(1, 0.75, 1)
            .displace(LucentShapes.noise(noiseSeed, 2))
            .displace(LucentShapes.jitter(jitterSeed, 1.5));
        int reach = (int) Math.ceil(radius + 4);
        int[] bounds = { -reach, -reach, -reach, reach, reach, reach };
        // The cut leaves a dome whose flat bottom passes through the tip, so the fill starts just above it.
        Set<Long> filled = new LinkedHashSet<>(SdfFill.fill(shape, 0, 1, 0, bounds));
        filled.addAll(SdfFill.fill(shape, 0, 2, 0, bounds));
        List<Long> leaves = new ArrayList<>();
        for (long c : filled) {
            long k = SdfFill.offset(c, cx, cy, cz);
            if (solid.get(k) == EndBlock.LUCENT_LOG) continue;
            solid.put(k, EndBlock.LUCENT_LEAVES);
            leaves.add(k);
        }
        for (long k : leaves) {
            if (r.nextInt(6) == 0 && !filled.contains(SdfFill.offset(k, -cx, -cy - 1, -cz)))
                hangs.add(SdfFill.offset(k, 0, -1, 0));
            if (r.nextInt(15) == 0 && deep(filled, SdfFill.offset(k, -cx, -cy, -cz))) solid.put(k, EndBlock.LUCENT_LOG);
        }
        solid.put(SdfFill.key(cx, cy, cz), EndBlock.LUCENT_LOG);
        // Upstream sets the four around the cell above; that cell joins them to the tip when the ball is thin.
        solid.put(SdfFill.key(cx, cy + 1, cz), EndBlock.LUCENT_LOG);
        for (int[] f : FACES) if (f[1] == 0) solid.put(SdfFill.key(cx + f[0], cy + 1, cz + f[2]), EndBlock.LUCENT_LOG);
    }

    /** No air within two steps along any axis, as upstream's check for wood inside the ball. */
    private static boolean deep(Set<Long> filled, long local) {
        for (int[] f : FACES) if (!filled.contains(SdfFill.offset(local, 2 * f[0], 2 * f[1], 2 * f[2]))) return false;
        return true;
    }

    /**
     * Vines and lanterns under the hanging points that still sit under a leaf: three in ten a short vine with a
     * lantern at its end, the rest a long vine with its glowing tip. A run stops above the tree's own blocks.
     */
    private static Map<Long, EndBlock> runs(Random r, List<Long> hangs, Map<Long, EndBlock> solid) {
        Map<Long, EndBlock> out = new LinkedHashMap<>();
        for (long top : hangs) {
            boolean lantern = r.nextDouble() < LANTERN_SHARE;
            int want = lantern ? 1 + r.nextInt(3) + 1 : 3 + r.nextInt(6);
            if (solid.get(SdfFill.offset(top, 0, 1, 0)) != EndBlock.LUCENT_LEAVES) continue;
            int room = 0;
            while (room < want) {
                long c = SdfFill.offset(top, 0, -room, 0);
                if (solid.containsKey(c) || out.containsKey(c)) break;
                room++;
            }
            if (room == 0) continue;
            for (int i = 0; i < room; i++) {
                EndBlock b = i < room - 1 ? EndBlock.LUCENT_VINE
                    : lantern && room >= 2 ? EndBlock.LUCENT_LANTERN : EndBlock.LUCENT_VINE_LAST;
                out.put(SdfFill.offset(top, 0, -i, 0), b);
            }
        }
        return out;
    }

    /** Logs, then leaves, then fur, then each hanging run top to bottom, ending in its tip or lantern. */
    public void forEach(CellVisitor v) {
        for (int i = 0; i < blocks.length; i++) v.cell(xs[i], ys[i], zs[i], blocks[i]);
    }

    public double size() {
        return size;
    }

    public int branches() {
        return branches;
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
