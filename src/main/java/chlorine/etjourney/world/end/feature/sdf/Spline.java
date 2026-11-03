package chlorine.etjourney.world.end.feature.sdf;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Polyline helpers after BCLib's SplineHelper: points as {x, y, z}, drawn as a chain of capsules. */
public final class Spline {

    private Spline() {}

    /** {@code points} points evenly spaced from (x1, y1, z1) to (x2, y2, z2). */
    public static List<double[]> make(double x1, double y1, double z1, double x2, double y2, double z2, int points) {
        List<double[]> spline = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double t = (double) i / (points - 1);
            spline.add(new double[] { x1 + (x2 - x1) * t, y1 + (y2 - y1) * t, z1 + (z2 - z1) * t });
        }
        return spline;
    }

    /**
     * Moves every point but the first by a Gaussian of the given spread per axis, keeping x and z within
     * {@code limit} of the first point.
     */
    public static void jitter(List<double[]> spline, Random random, double dx, double dy, double dz, double limit) {
        double[] first = spline.get(0);
        for (int i = 1; i < spline.size(); i++) {
            double[] p = spline.get(i);
            p[0] = clamp(p[0] + random.nextGaussian() * dx, first[0] - limit, first[0] + limit);
            p[1] += random.nextGaussian() * dy;
            p[2] = clamp(p[2] + random.nextGaussian() * dz, first[2] - limit, first[2] + limit);
        }
    }

    /** Capsules between successive points, their radius running from {@code first} to {@code last}. */
    public static Sdf shape(List<double[]> spline, double first, double last) {
        Sdf result = null;
        int segments = spline.size() - 1;
        for (int i = 0; i < segments; i++) {
            double[] a = spline.get(i), b = spline.get(i + 1);
            double r = segments == 1 ? first : first + (last - first) * i / (segments - 1);
            Sdf segment = Sdf.line(a[0], a[1], a[2], b[0], b[1], b[2], r);
            result = result == null ? segment : result.union(segment);
        }
        return result;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
