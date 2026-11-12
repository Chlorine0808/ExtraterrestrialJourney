package chlorine.etjourney.content.worm;

import java.util.ArrayDeque;
import java.util.Iterator;

/** The path a worm's head took, newest point first, kept only as long as the body. */
public final class WormTrail {

    private final double length;
    /** A step longer than this is a teleport, not swimming. */
    private final double jumpLimit;
    private final ArrayDeque<double[]> points = new ArrayDeque<>();

    public WormTrail(double length, double jumpLimit) {
        this.length = length;
        this.jumpLimit = jumpLimit;
    }

    /** Adds the head's new spot; a teleport lays a fresh straight trail instead of stretching the old one. */
    public void record(double x, double y, double z) {
        double[] head = points.peekFirst();
        if (head != null && dist(head, x, y, z) < 1e-6) return;
        if (head != null && dist(head, x, y, z) > jumpLimit) {
            double[] dir = heading();
            lay(x, y, z, dir[0], dir[1], dir[2]);
            return;
        }
        points.addFirst(new double[] { x, y, z });
        prune();
    }

    /** Replaces the trail with a straight line running back from the head against the facing. */
    public void lay(double x, double y, double z, double fx, double fy, double fz) {
        double n = Math.sqrt(fx * fx + fy * fy + fz * fz);
        if (n < 1e-9) {
            fx = 0;
            fy = 0;
            fz = 1;
            n = 1;
        }
        points.clear();
        points.addLast(new double[] { x, y, z });
        points.addLast(new double[] { x - fx / n * length, y - fy / n * length, z - fz / n * length });
    }

    /** The point the given distance back along the trail; the oldest point when the trail is shorter. */
    public double[] at(double distance) {
        Iterator<double[]> it = points.iterator();
        if (!it.hasNext()) return new double[3];
        double[] a = it.next();
        double left = distance;
        while (it.hasNext()) {
            double[] b = it.next();
            double step = dist(a, b[0], b[1], b[2]);
            if (step >= left && step > 0) {
                double t = left / step;
                return new double[] { a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t };
            }
            left -= step;
            a = b;
        }
        return a.clone();
    }

    public int size() {
        return points.size();
    }

    /** The direction of the newest step, or +z when there is none. */
    private double[] heading() {
        Iterator<double[]> it = points.iterator();
        double[] a = it.next();
        while (it.hasNext()) {
            double[] b = it.next();
            if (dist(a, b[0], b[1], b[2]) > 1e-9) return new double[] { a[0] - b[0], a[1] - b[1], a[2] - b[2] };
        }
        return new double[] { 0, 0, 1 };
    }

    private void prune() {
        Iterator<double[]> it = points.iterator();
        double[] a = it.next();
        double walked = 0;
        while (it.hasNext()) {
            double[] b = it.next();
            if (walked >= length) {
                it.remove();
                continue;
            }
            walked += dist(a, b[0], b[1], b[2]);
            a = b;
        }
    }

    private static double dist(double[] a, double x, double y, double z) {
        double dx = a[0] - x, dy = a[1] - y, dz = a[2] - z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
