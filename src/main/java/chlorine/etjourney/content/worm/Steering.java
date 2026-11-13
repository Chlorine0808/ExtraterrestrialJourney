package chlorine.etjourney.content.worm;

/** Turning a heading towards a wanted one by a capped angle. */
public final class Steering {

    private Steering() {}

    /**
     * The new unit heading, at most {@code maxAngle} radians from {@code current} towards {@code wanted}.
     * Either vector may have any length; a zero current heading takes the wanted one at once.
     */
    public static double[] turn(double[] current, double[] wanted, double maxAngle) {
        double[] w = unit(wanted);
        double cn = norm(current);
        if (cn < 1e-9 || w == null) return w != null ? w : new double[] { 0, 0, 1 };
        double[] c = { current[0] / cn, current[1] / cn, current[2] / cn };
        double cos = Math.max(-1, Math.min(1, c[0] * w[0] + c[1] * w[1] + c[2] * w[2]));
        double angle = Math.acos(cos);
        if (angle <= maxAngle) return w;
        // The part of the wanted heading square to the current one; any square direction when they are opposite.
        double[] side = { w[0] - c[0] * cos, w[1] - c[1] * cos, w[2] - c[2] * cos };
        double[] s = unit(side);
        if (s == null) s = unit(new double[] { -c[2], 0, c[0] });
        if (s == null) s = new double[] { 1, 0, 0 };
        double sin = Math.sin(maxAngle), co = Math.cos(maxAngle);
        return new double[] { c[0] * co + s[0] * sin, c[1] * co + s[1] * sin, c[2] * co + s[2] * sin };
    }

    private static double norm(double[] v) {
        return Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    }

    private static double[] unit(double[] v) {
        double n = norm(v);
        return n < 1e-9 ? null : new double[] { v[0] / n, v[1] / n, v[2] / n };
    }
}
