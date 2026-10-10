package chlorine.etjourney.world.end.feature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import chlorine.etjourney.world.end.noise.CellCache;
import chlorine.etjourney.world.end.noise.Hash;
import chlorine.etjourney.world.end.noise.ValueNoise;

/**
 * The ARCS style: thick tubes along winding 3D paths, so walking them feels like climbing the branches of a
 * giant tree. Each path turns steadily about an axis (an arc or loop when calm) and is jostled by a wiggle, so calm
 * paths read as circles and restless ones as tangled vines. Paths are polylines of STEP-block segments.
 */
public final class ArcPaths {

    public static final int CELL = 80;
    private static final int STEP = 4;
    /** Paths per cell: 0.8 on average, times a per-cell factor of 1 to 4. */
    private static final double BASE_COUNT = 0.8, MAX_COUNT_FACTOR = 4;
    /** Share of the paths ARCS draws where it is laid over another base rather than standing as the base. */
    private static final double OVERLAY_COUNT = 1 / 3.0;
    /** Path length: 120 blocks times 0.5 to 8 (log-uniform, so short and long are equally common). */
    private static final double BASE_LENGTH = 120, MIN_LENGTH_FACTOR = 0.5, MAX_LENGTH_FACTOR = 8;
    /** Wiggle: 0.5 to 10 times the base jostle (log-uniform). */
    private static final double MIN_WIGGLE = 0.5, MAX_WIGGLE = 10;
    /** Tube radius: 7.5 times 0.8 to 1.5, never under 6 so the 8-block density grid resolves vertical runs. */
    private static final double BASE_TUBE = 7.5, MIN_TUBE = 6;
    /** Paths turn back once they stray this far from their start, which bounds the search. */
    private static final double MAX_REACH = 420;
    private static final double MIN_Y = 6, MAX_Y = 250;
    /**
     * Across the region border a tube narrows to END_TUBE over TAPER of ARCS weight above its path's cutoff, and
     * stops below it. Thinner tubes fall between the 8-block density grid nodes and break up.
     */
    static final double END_TUBE = 4, TAPER = 0.3;
    /** Range of the per-path cutoff, so paths end at different depths into the border. */
    private static final double MIN_CUTOFF = 0.02, MAX_CUTOFF = 0.35;
    private static final long SALT = 0x8A5CD789635D2DFFL;

    private static final CellCache<List<Path>> CELLS = new CellCache<>(32768);

    private ArcPaths() {}

    /** Weight of the ARCS style at a point, supplied by the engine. */
    public interface Probe {

        double weight(double x, double z);

        /** The part of the weight that comes from ARCS as the base rather than as an overlay. */
        default double baseWeight(double x, double z) {
            return weight(x, z);
        }
    }

    /** One path: its points (x, y, z triples), tube radius and cutoff, with a horizontal bounding box. */
    public static final class Path {

        final double[] points;
        public final double tube;
        /** ARCS weight at or below which the path stops. */
        final double cutoff;
        final double minX, maxX, minZ, maxZ;
        /** Answers of the last shared weight, per segment; NaN when not yet asked. */
        private volatile Memo memo;

        /** A path that keeps its full tube wherever the ARCS weight is above 0. */
        Path(double[] points, double tube) {
            this(points, tube, 0);
        }

        Path(double[] points, double tube, double cutoff) {
            this.points = points;
            this.tube = tube;
            this.cutoff = cutoff;
            double x0 = Double.MAX_VALUE, x1 = -Double.MAX_VALUE, z0 = Double.MAX_VALUE, z1 = -Double.MAX_VALUE;
            for (int i = 0; i < points.length; i += 3) {
                x0 = Math.min(x0, points[i]);
                x1 = Math.max(x1, points[i]);
                z0 = Math.min(z0, points[i + 2]);
                z1 = Math.max(z1, points[i + 2]);
            }
            minX = x0 - tube;
            maxX = x1 + tube;
            minZ = z0 - tube;
            maxZ = z1 + tube;
        }

        /** Start of the path, as {x, y, z}. */
        public double[] start() {
            return new double[] { points[0], points[1], points[2] };
        }

        /** Tube radius at an ARCS weight, or 0 where the path stops. */
        double tubeAt(double weight) {
            if (weight <= cutoff) return 0;
            double t = ValueNoise.smooth(Math.min(1, (weight - cutoff) / TAPER));
            return END_TUBE + (tube - END_TUBE) * t;
        }

        private Memo memoFor(SegmentWeight filter) {
            Memo m = memo;
            if (m == null || m.filter != filter) {
                float[] answers = new float[points.length / 3 - 1];
                Arrays.fill(answers, Float.NaN);
                m = new Memo(filter, answers);
                memo = m;
            }
            return m;
        }
    }

    private static final class Memo {

        final SegmentWeight filter;
        final float[] answers;

        Memo(SegmentWeight filter, float[] answers) {
            this.filter = filter;
            this.answers = answers;
        }
    }

    /** Segments near a chunk, flattened: ax, ay, az, bx, by, bz, tube per segment. */
    public static final class Segments {

        final double[] data;
        final int count;

        Segments(double[] data, int count) {
            this.data = data;
            this.count = count;
        }

        /** Lowest point a tube reaches, or 0 when there are no segments. */
        public double minY() {
            double low = Double.MAX_VALUE;
            for (int k = 0; k < count; k++) {
                low = Math.min(low, Math.min(data[k * 7 + 1], data[k * 7 + 4]) - data[k * 7 + 6]);
            }
            return count == 0 ? 0 : low;
        }

        /** Highest point a tube reaches, or 0 when there are no segments. */
        public double maxY() {
            double high = -Double.MAX_VALUE;
            for (int k = 0; k < count; k++) {
                high = Math.max(high, Math.max(data[k * 7 + 1], data[k * 7 + 4]) + data[k * 7 + 6]);
            }
            return count == 0 ? 0 : high;
        }

        public boolean isEmpty() {
            return count == 0;
        }

        /** The segments that reach the box, the only ones that can add density inside it. */
        public Segments within(double minX, double maxX, double minZ, double maxZ) {
            double[] out = new double[count * 7];
            int n = 0;
            for (int k = 0; k < count; k++) {
                int o = k * 7;
                double pad = data[o + 6] + REACH;
                if (Math.max(data[o], data[o + 3]) + pad < minX || Math.min(data[o], data[o + 3]) - pad > maxX)
                    continue;
                if (Math.max(data[o + 2], data[o + 5]) + pad < minZ || Math.min(data[o + 2], data[o + 5]) - pad > maxZ)
                    continue;
                System.arraycopy(data, o, out, n * 7, 7);
                n++;
            }
            return new Segments(out, n);
        }
    }

    /** Distance outside a tube beyond which it adds no density. */
    static final double REACH = 16;

    public interface SegmentFilter {

        boolean keep(double x, double y, double z);
    }

    /** ARCS weight for a segment at its midpoint; 0 drops it. */
    public interface SegmentWeight {

        double weight(double x, double y, double z);
    }

    /** Paths whose bounding box comes within range of (x, z). */
    public static List<Path> pathsNear(long seed, double x, double z, double range, Probe probe) {
        List<Path> out = new ArrayList<>();
        double reach = range + MAX_REACH + BASE_TUBE * 1.5;
        int c0x = (int) Math.floor((x - reach) / CELL), c1x = (int) Math.floor((x + reach) / CELL);
        int c0z = (int) Math.floor((z - reach) / CELL), c1z = (int) Math.floor((z + reach) / CELL);
        for (int cx = c0x; cx <= c1x; cx++) {
            for (int cz = c0z; cz <= c1z; cz++) {
                for (Path path : pathsInCell(seed, cx, cz, probe)) {
                    if (path.maxX >= x - range && path.minX <= x + range
                        && path.maxZ >= z - range
                        && path.minZ <= z + range) out.add(path);
                }
            }
        }
        return out;
    }

    public static List<Path> pathsInCell(long seed, int cx, int cz, Probe probe) {
        List<Path> paths = CELLS.get(seed, cx, cz, (sd, i, j) -> computePaths(sd, i, j, probe));
        return paths == null ? Collections.emptyList() : paths;
    }

    private static List<Path> computePaths(long seed, int cx, int cz, Probe probe) {
        long s = seed ^ SALT;
        double x = (cx + 0.5) * CELL, z = (cz + 0.5) * CELL;
        if (Math.hypot(x, z) < 1000 || !Fade.forms(probe.weight(x, z), Hash.hash01(s + 2, cx, cz)))
            return Collections.emptyList();
        double factor = 1 + (MAX_COUNT_FACTOR - 1) * Hash.hash01(s, cx, cz);
        // Laid over another base, ARCS only accents it, so it draws fewer paths.
        double weight = probe.weight(x, z), base = Math.min(1, probe.baseWeight(x, z) / weight);
        factor *= base + (1 - base) * OVERLAY_COUNT;
        int count = (int) Math.floor(BASE_COUNT * factor + Hash.hash01(s + 1, cx, cz));
        List<Path> out = new ArrayList<>();
        Random r = new Random(s ^ ((long) cx * 0x9E3779B97F4A7C15L) ^ ((long) cz * 0xC2B2AE3D27D4EB4FL));
        for (int i = 0; i < count; i++) {
            double cutoff = MIN_CUTOFF + (MAX_CUTOFF - MIN_CUTOFF) * Hash.hash01(s + 3 + i, cx, cz);
            out.add(walk(r, (cx + r.nextDouble()) * CELL, (cz + r.nextDouble()) * CELL, cutoff));
        }
        return out;
    }

    /** Walks one path from (x, z): a steady turn about a drifting axis, jostled by the wiggle. */
    private static Path walk(Random r, double x, double z, double cutoff) {
        double length = BASE_LENGTH * MIN_LENGTH_FACTOR
            * Math.pow(MAX_LENGTH_FACTOR / MIN_LENGTH_FACTOR, r.nextDouble());
        double wiggle = MIN_WIGGLE * Math.pow(MAX_WIGGLE / MIN_WIGGLE, r.nextDouble());
        double tube = Math.max(MIN_TUBE, BASE_TUBE * (0.8 + 0.7 * r.nextDouble()));
        double turnRadius = 20 + 50 * r.nextDouble();
        double turn = STEP / turnRadius * (r.nextBoolean() ? 1 : -1);
        double y = MIN_Y + tube + (MAX_Y - MIN_Y - 2 * tube) * r.nextDouble();
        double[] dir = normalize(new double[] { r.nextGaussian(), r.nextGaussian() * 0.8, r.nextGaussian() });
        double[] axis = normalize(new double[] { r.nextGaussian(), r.nextGaussian() * 1.5, r.nextGaussian() });
        int steps = Math.max(2, (int) (length / STEP));
        double[] points = new double[(steps + 1) * 3];
        double px = x, py = y, pz = z;
        points[0] = px;
        points[1] = py;
        points[2] = pz;
        for (int i = 1; i <= steps; i++) {
            dir = rotate(dir, axis, turn);
            axis = normalize(add(axis, gaussian(r, 0.02 * wiggle)));
            dir = normalize(add(dir, gaussian(r, 0.03 * wiggle)));
            // Bounce off the floor and ceiling, and head home once too far out.
            if (py < MIN_Y + tube + 4 && dir[1] < 0 || py > MAX_Y - tube - 4 && dir[1] > 0) dir[1] = -dir[1];
            if (Math.hypot(px - x, pz - z) > MAX_REACH - 40) {
                dir = normalize(add(dir, new double[] { (x - px) * 0.01, 0, (z - pz) * 0.01 }));
            }
            px += dir[0] * STEP;
            py = Math.max(MIN_Y + tube, Math.min(MAX_Y - tube, py + dir[1] * STEP));
            pz += dir[2] * STEP;
            points[i * 3] = px;
            points[i * 3 + 1] = py;
            points[i * 3 + 2] = pz;
        }
        return new Path(points, tube, cutoff);
    }

    /** Segments of the paths that come within reach of the horizontal box, skipping those keep rejects. */
    public static Segments segmentsNear(List<Path> paths, double minX, double maxX, double minZ, double maxZ,
        SegmentFilter keep) {
        return segmentsNear(paths, minX, maxX, minZ, maxZ, null, keep);
    }

    /**
     * As above, with a shared ARCS weight that depends on the segment alone and narrows the tubes across the border.
     * Its answers are kept on the path for as long as the same instance is passed, so neighbouring chunks do not
     * repeat it.
     */
    public static Segments segmentsNear(List<Path> paths, double minX, double maxX, double minZ, double maxZ,
        SegmentWeight shared, SegmentFilter keep) {
        double[] data = new double[64 * 7];
        int n = 0;
        for (Path path : paths) {
            double[] p = path.points;
            float[] answers = shared == null ? null : path.memoFor(shared).answers;
            for (int i = 0; i + 5 < p.length; i += 3) {
                double lo = Math.min(p[i], p[i + 3]) - path.tube, hi = Math.max(p[i], p[i + 3]) + path.tube;
                if (hi < minX || lo > maxX) continue;
                lo = Math.min(p[i + 2], p[i + 5]) - path.tube;
                hi = Math.max(p[i + 2], p[i + 5]) + path.tube;
                if (hi < minZ || lo > maxZ) continue;
                double mx = (p[i] + p[i + 3]) / 2, my = (p[i + 1] + p[i + 4]) / 2, mz = (p[i + 2] + p[i + 5]) / 2;
                double tube = path.tube;
                if (answers != null) {
                    int k = i / 3;
                    if (Float.isNaN(answers[k])) answers[k] = (float) shared.weight(mx, my, mz);
                    tube = path.tubeAt(answers[k]);
                    if (tube <= 0) continue;
                }
                if (!keep.keep(mx, my, mz)) continue;
                if ((n + 1) * 7 > data.length) data = Arrays.copyOf(data, data.length * 2);
                System.arraycopy(p, i, data, n * 7, 6);
                data[n * 7 + 6] = tube;
                n++;
            }
        }
        return new Segments(data, n);
    }

    /**
     * Density of the tubes at a point: positive inside one, roughly the distance to its surface. Tubes further than
     * REACH from their surface add nothing, so a chunk only needs the segments near it.
     */
    public static double density(Segments segments, double x, double y, double z) {
        double best = Double.NEGATIVE_INFINITY;
        double[] d = segments.data;
        for (int k = 0; k < segments.count; k++) {
            int o = k * 7;
            double ax = d[o], ay = d[o + 1], az = d[o + 2];
            double ex = d[o + 3] - ax, ey = d[o + 4] - ay, ez = d[o + 5] - az;
            double t = ((x - ax) * ex + (y - ay) * ey + (z - az) * ez) / (ex * ex + ey * ey + ez * ez + 1e-9);
            t = Math.max(0, Math.min(1, t));
            double qx = ax + ex * t - x, qy = ay + ey * t - y, qz = az + ez * t - z;
            double v = d[o + 6] - Math.sqrt(qx * qx + qy * qy + qz * qz);
            if (v >= -REACH) best = Math.max(best, v);
        }
        return best;
    }

    /** Rodrigues' rotation of v about the unit axis k. */
    private static double[] rotate(double[] v, double[] k, double angle) {
        double c = Math.cos(angle), s = Math.sin(angle);
        double dot = v[0] * k[0] + v[1] * k[1] + v[2] * k[2];
        double[] cross = { k[1] * v[2] - k[2] * v[1], k[2] * v[0] - k[0] * v[2], k[0] * v[1] - k[1] * v[0] };
        return new double[] { v[0] * c + cross[0] * s + k[0] * dot * (1 - c),
            v[1] * c + cross[1] * s + k[1] * dot * (1 - c), v[2] * c + cross[2] * s + k[2] * dot * (1 - c) };
    }

    private static double[] gaussian(Random r, double sigma) {
        return new double[] { r.nextGaussian() * sigma, r.nextGaussian() * sigma, r.nextGaussian() * sigma };
    }

    private static double[] add(double[] a, double[] b) {
        return new double[] { a[0] + b[0], a[1] + b[1], a[2] + b[2] };
    }

    private static double[] normalize(double[] a) {
        double l = Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
        if (l < 1e-9) return new double[] { 1, 0, 0 };
        return new double[] { a[0] / l, a[1] / l, a[2] / l };
    }
}
