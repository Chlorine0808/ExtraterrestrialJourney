package chlorine.etjourney.world.end.feature.plant;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import chlorine.etjourney.world.end.feature.sdf.Sdf;
import chlorine.etjourney.world.end.feature.sdf.SdfFill;
import chlorine.etjourney.world.end.feature.sdf.Spline;
import chlorine.etjourney.world.end.modifier.EndBlock;
import chlorine.etjourney.world.end.noise.OpenSimplex2D;

/**
 * BetterEnd's mossy glowshroom at full size (a cap about 36 wide at scale 1), built from the same SDF graph as
 * {@code MossyGlowshroomFeature} and {@code GiantMossyGlowshroomStructure}.
 */
public final class Glowshroom {

    /** Blocks the stem's foot reaches below the root. */
    public static final int SINK = 6;

    private static final double MIN_SCALE = 0.5;
    private static final double MIN_SCALE_GIANT = 1.5;
    private static final int FIT_TRIES = 12;

    public interface CellVisitor {

        void cell(int dx, int dy, int dz, EndBlock block);
    }

    private final int[] xs, ys, zs;
    private final EndBlock[] blocks;
    private final double scale;
    private final int stemLength;
    private final int minX, maxX, minY, maxY, minZ, maxZ;

    private Glowshroom(List<int[]> cells, List<EndBlock> kinds, double scale, int stemLength) {
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
        this.scale = scale;
        this.stemLength = stemLength;
    }

    /**
     * Builds and fills the mushroom; null when it has to shrink below the minimum scale to fit under {@code maxTop}.
     */
    public static Glowshroom of(long shapeSeed, double scale, boolean giant, int maxTop) {
        double min = giant ? MIN_SCALE_GIANT : MIN_SCALE;
        for (int i = 0; i <= FIT_TRIES && scale >= min; i++, scale *= 0.9) {
            Glowshroom g = build(shapeSeed, scale, giant);
            if (g.maxY <= maxTop) return g;
        }
        return null;
    }

    private static Glowshroom build(long shapeSeed, double scale, boolean giant) {
        Random r = new Random(shapeSeed);
        double length = 10 + r.nextDouble() * 15;
        List<double[]> spline = Spline.make(0, 0, 0, 0, length, 0, Math.max(2, (int) Math.floor(length / 4)));
        Spline.jitter(spline, r, 1, 0, 1, Double.MAX_VALUE);
        double[] top = spline.get(spline.size() - 1);
        double angle = r.nextDouble() * Math.PI * 2;
        OpenSimplex2D noise = new OpenSimplex2D(r.nextLong());

        Sdf stem = Spline.shape(spline, 2.1, 1.5)
            .union(Sdf.line(0, -SINK, 0, 0, 0, 0, 2.1));

        Sdf cone1 = Sdf.cappedCone(2.5, 1.5, 2.5);
        Sdf cone2 = Sdf.cappedCone(3, 2.5, 13);
        Sdf posedCone3 = cone2.scale(2)
            .translate(0, 12, 0);
        Sdf upCone = cone2.translate(0, 5, 0)
            .subtract(posedCone3);
        Sdf wave = upCone.flatWave(12, 1.3, 0);
        Sdf capCones = cone1.smoothUnion(wave, 3)
            .union(
                upCone.translate(0, 1.25, 0)
                    .scale(1.2, 1, 1.2));
        Sdf gill = Sdf.cappedCone(3, 2, 12.5)
            .translate(0, 4.25, 0)
            .subtract(posedCone3);

        Sdf cap = pose(capCones, noise, top);
        Sdf glowRaw = pose(
            Sdf.cappedCone(3, 2, 12.5)
                .translate(0, 4.25, 0),
            noise,
            top);
        Sdf lid = pose(posedCone3, noise, top);
        Sdf gills = pose(gill, noise, top);
        Sdf roots = Sdf.sphere(4)
            .scale(1, 0.7, 1)
            .flatWave(5, 1.5, angle);

        Sdf whole = stem.smoothUnion(
            cap.union(gills)
                .union(roots),
            4)
            .scale(scale);
        if (giant) whole = whole.round(1.5);

        int half = (int) Math.ceil(22 * scale) + 2;
        int[] bounds = { -half, -SINK - (int) Math.ceil(3 * scale), -half, half,
            (int) Math.ceil((length + 20) * scale) + 2, half };
        Set<Long> filled = SdfFill.fill(whole, 0, 0, 0, bounds);

        List<int[]> cells = new ArrayList<>();
        List<EndBlock> kinds = new ArrayList<>();
        List<int[]> gillCells = new ArrayList<>();
        for (long c : filled) {
            int x = SdfFill.x(c), y = SdfFill.y(c), z = SdfFill.z(c);
            EndBlock block = block(x / scale, y / scale, z / scale, stem, roots, cap, gills, glowRaw, lid);
            int[] cell = { x, y, z };
            cells.add(cell);
            kinds.add(block);
            if (block == EndBlock.MYCELIA_GILL) gillCells.add(cell);
        }
        for (int[] g : gillCells) {
            if (filled.contains(SdfFill.key(g[0], g[1] - 1, g[2]))) continue;
            int run = 1 + Math.floorMod(hash(shapeSeed, g[0], g[1], g[2]), 3);
            int placed = 0;
            while (placed < run && !filled.contains(SdfFill.key(g[0], g[1] - 1 - placed, g[2]))) placed++;
            for (int i = 1; i <= placed; i++) {
                cells.add(new int[] { g[0], g[1] - i, g[2] });
                kinds.add(i == placed ? EndBlock.MYCELIA_VINE_LAST : EndBlock.MYCELIA_VINE);
            }
        }
        return new Glowshroom(cells, kinds, scale, (int) length);
    }

    /**
     * Which block a point gets, choosing as BCLib does: each union takes the material of the nearer operand, and
     * a subtraction takes its first operand's unless the subtrahend is nearer, in which case it is the lid cone's
     * (the cap block).
     */
    private static EndBlock block(double x, double y, double z, Sdf stem, Sdf roots, Sdf cap, Sdf gills, Sdf glowRaw,
        Sdf lid) {
        double glow = gills.distance(x, y, z);
        EndBlock glowBlock = glowRaw.distance(x, y, z) < lid.distance(x, y, z) ? EndBlock.MYCELIA_GILL
            : EndBlock.MYCELIA_CAP;
        double capD = cap.distance(x, y, z);
        EndBlock headBlock = capD < glow ? EndBlock.MYCELIA_CAP : glowBlock;
        double head = Math.min(capD, glow);
        EndBlock topBlock = head < roots.distance(x, y, z) ? headBlock : EndBlock.MYCELIA_STEM;
        double top = Math.min(head, roots.distance(x, y, z));
        return stem.distance(x, y, z) < top ? EndBlock.MYCELIA_STEM : topBlock;
    }

    /** Warps the head as upstream does, then seats it on the stem top. */
    private static Sdf pose(Sdf head, OpenSimplex2D noise, double[] top) {
        return head.warp(p -> {
            double d = Math.sqrt(p[0] * p[0] + p[2] * p[2]);
            double n = noise.eval(p[0] * 0.1, p[2] * 0.1);
            p[1] += n * d * 0.3 - d * 0.15;
        })
            .translate(0, 2.5, 0)
            .translate(top[0], top[1], top[2]);
    }

    private static int hash(long seed, int x, int y, int z) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL) ^ (z * 0x165667B19E3779F9L);
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        return (int) ((h ^ (h >>> 31)) >>> 33);
    }

    /** Stem, cap and gill cells first, then the fur, each run top to bottom. */
    public void forEach(CellVisitor v) {
        for (int i = 0; i < blocks.length; i++) v.cell(xs[i], ys[i], zs[i], blocks[i]);
    }

    public double scale() {
        return scale;
    }

    public int stemLength() {
        return stemLength;
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
