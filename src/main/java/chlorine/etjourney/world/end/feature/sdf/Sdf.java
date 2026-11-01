package chlorine.etjourney.world.end.feature.sdf;

import java.util.function.Consumer;

/**
 * A signed distance shape: negative inside, positive outside. The primitives and operators follow BCLib's
 * {@code org.betterx.bclib.sdf} (MIT License, Copyright (c) 2020 paulevsGitch).
 */
@FunctionalInterface
public interface Sdf {

    double distance(double x, double y, double z);

    /** Adds a value to the distance at a point; a negative value grows the shape there. */
    @FunctionalInterface
    interface Displacement {

        double at(double x, double y, double z);
    }

    static Sdf sphere(double radius) {
        return (x, y, z) -> Math.sqrt(x * x + y * y + z * z) - radius;
    }

    /** A capsule from (x1, y1, z1) to (x2, y2, z2). */
    static Sdf line(double x1, double y1, double z1, double x2, double y2, double z2, double radius) {
        double bx = x2 - x1, by = y2 - y1, bz = z2 - z1;
        double bb = bx * bx + by * by + bz * bz;
        return (x, y, z) -> {
            double px = x - x1, py = y - y1, pz = z - z1;
            double h = bb == 0 ? 0 : Math.max(0, Math.min(1, (px * bx + py * by + pz * bz) / bb));
            double dx = px - bx * h, dy = py - by * h, dz = pz - bz * h;
            return Math.sqrt(dx * dx + dy * dy + dz * dz) - radius;
        };
    }

    /** A cone cut flat at y = -height (radius bottom) and y = +height (radius top). */
    static Sdf cappedCone(double height, double bottom, double top) {
        double k2x = top - bottom, k2y = 2 * height, kk = k2x * k2x + k2y * k2y;
        return (x, y, z) -> {
            double q = Math.sqrt(x * x + z * z);
            double cax = q - Math.min(q, y < 0 ? bottom : top);
            double cay = Math.abs(y) - height;
            double t = Math.max(0, Math.min(1, ((top - q) * k2x + (height - y) * k2y) / kk));
            double cbx = q - top + k2x * t;
            double cby = y - height + k2y * t;
            double s = cbx < 0 && cay < 0 ? -1 : 1;
            return s * Math.sqrt(Math.min(cax * cax + cay * cay, cbx * cbx + cby * cby));
        };
    }

    default Sdf translate(double dx, double dy, double dz) {
        return (x, y, z) -> distance(x - dx, y - dy, z - dz);
    }

    /** Stretches the shape per axis; the result is no longer an exact distance, only its sign is. */
    default Sdf scale(double sx, double sy, double sz) {
        return (x, y, z) -> distance(x / sx, y / sy, z / sz);
    }

    default Sdf union(Sdf other) {
        return (x, y, z) -> Math.min(distance(x, y, z), other.distance(x, y, z));
    }

    /** A union that rounds the seam over about {@code radius} blocks. */
    default Sdf smoothUnion(Sdf other, double radius) {
        return (x, y, z) -> {
            double a = distance(x, y, z), b = other.distance(x, y, z);
            double h = Math.max(0, Math.min(1, 0.5 + 0.5 * (b - a) / radius));
            return b + (a - b) * h - radius * h * (1 - h);
        };
    }

    default Sdf subtract(Sdf other) {
        return (x, y, z) -> Math.max(distance(x, y, z), -other.distance(x, y, z));
    }

    default Sdf displace(Displacement by) {
        return (x, y, z) -> distance(x, y, z) + by.at(x, y, z);
    }

    /** Ripples the shape around the vertical axis with {@code rays} lobes. */
    default Sdf flatWave(int rays, double intensity, double angle) {
        return displace((x, y, z) -> Math.cos(Math.atan2(x, z) * rays + angle) * intensity);
    }

    /** Measures at a moved point: {@code move} edits {x, y, z} in place. */
    default Sdf warp(Consumer<double[]> move) {
        return (x, y, z) -> {
            double[] p = { x, y, z };
            move.accept(p);
            return distance(p[0], p[1], p[2]);
        };
    }
}
