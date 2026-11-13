package chlorine.etjourney.content.projectile;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Unit directions for firing several shots at once. */
public final class Spread {

    private Spread() {}

    /** {@code count} directions turned evenly across {@code angle} radians about the vertical, centred on dir. */
    public static List<double[]> fan(double[] dir, int count, double angle) {
        double[] d = unit(dir);
        List<double[]> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double a = count == 1 ? 0 : -angle / 2 + angle * i / (count - 1);
            double c = Math.cos(a), s = Math.sin(a);
            out.add(new double[] { d[0] * c - d[2] * s, d[1], d[0] * s + d[2] * c });
        }
        return out;
    }

    /** One direction at random within {@code halfAngle} radians of axis, spread evenly over the cone's cap. */
    public static double[] cone(double[] axis, double halfAngle, Random random) {
        double[] a = unit(axis);
        double cos = 1 - random.nextDouble() * (1 - Math.cos(halfAngle));
        double sin = Math.sqrt(Math.max(0, 1 - cos * cos));
        double turn = random.nextDouble() * 2 * Math.PI;
        // Two unit vectors square to the axis and to each other.
        double[] helper = Math.abs(a[1]) < 0.9 ? new double[] { 0, 1, 0 } : new double[] { 1, 0, 0 };
        double[] u = unit(cross(a, helper));
        double[] w = cross(a, u);
        double cu = Math.cos(turn) * sin, cw = Math.sin(turn) * sin;
        return new double[] { a[0] * cos + u[0] * cu + w[0] * cw, a[1] * cos + u[1] * cu + w[1] * cw,
            a[2] * cos + u[2] * cu + w[2] * cw };
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] { a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0] };
    }

    private static double[] unit(double[] v) {
        double n = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        if (n < 1e-9) return new double[] { 0, 0, 1 };
        return new double[] { v[0] / n, v[1] / n, v[2] / n };
    }
}
