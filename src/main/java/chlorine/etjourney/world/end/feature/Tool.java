package chlorine.etjourney.world.end.feature;

import chlorine.etjourney.world.end.noise.Hash;

/**
 * A giant tool in CROSSES regions: a tool shape blown up to 2-5 blocks a pixel. Over land most are driven in at a lean
 * by their planted end; the rest float at any angle. Drawn block by block.
 */
public final class Tool extends Structure {

    public static final Structure.Kind<Tool> KIND = new Structure.Kind<Tool>("CROSSES", 112, 64) {

        @Override
        protected Tool compute(long seed, int cx, int cz, StructureProbe probe) {
            long s = seed ^ 0x2E6C9A1F5B3D7048L;
            if (Hash.hash01(s, cx, cz) > 0.5) return null;
            double x = (cx + 0.2 + 0.6 * Hash.hash01(s + 1, cx, cz)) * cell;
            double z = (cz + 0.2 + 0.6 * Hash.hash01(s + 2, cx, cz)) * cell;
            if (!Structures.forms(s, cx, cz, probe.weight(style, x, z))) return null;
            ToolShape[] shapes = ToolShape.values();
            ToolShape shape = shapes[(int) (Hash.hash01(s + 3, cx, cz) * shapes.length)];
            // More small tools than large ones.
            double scale = 2 + 3 * Math.pow(Hash.hash01(s + 4, cx, cz), 1.5);
            double ground = probe.ground(x, z);
            boolean grounded = ground > -100 && Hash.hash01(s + 5, cx, cz) < 0.6;
            double lean = Math.toRadians((grounded ? 25 : 70) * Hash.hash01(s + 6, cx, cz));
            double yaw = Hash.hash01(s + 7, cx, cz) * Math.PI * 2, twist = Hash.hash01(s + 8, cx, cz) * Math.PI * 2;
            double[] up = { Math.sin(lean) * Math.cos(yaw), Math.cos(lean), Math.sin(lean) * Math.sin(yaw) };
            double[] side = normalize(cross(up, new double[] { Math.cos(twist), 0, Math.sin(twist) }));
            // A tool driven in tip first hangs its head downwards.
            double[] axis = shape.tipFirst ? scaled(up, -1) : up;
            double[] normal = cross(side, axis);
            double[] b = shape.bounds();
            double[] origin = new double[3];
            if (grounded) {
                // An eighth of its length buried below the planted end.
                double[] planted = shape.planted();
                double buried = (b[3] - b[2]) * scale * 0.12;
                double[] foot = { x, ground, z };
                for (int i = 0; i < 3; i++) {
                    origin[i] = foot[i] - up[i] * buried - (side[i] * planted[0] + axis[i] * planted[1]) * scale;
                }
            } else {
                double[] mid = { (b[0] + b[1]) / 2, (b[2] + b[3]) / 2, (b[4] + b[5]) / 2 };
                double[] around = Tool.of(shape, new double[3], side, axis, normal, scale, null)
                    .extent(mid);
                double low = (ground > -100 ? ground + 6 : Structures.VOID_BASE) - around[0];
                double high = Structures.CEILING - around[1];
                if (low > high) return null;
                double y = low + Math.min(high - low, 20 + 120 * Hash.hash01(s + 9, cx, cz));
                double[] centre = { x, y, z };
                for (int i = 0; i < 3; i++) {
                    origin[i] = centre[i] - (side[i] * mid[0] + axis[i] * mid[1] + normal[i] * mid[2]) * scale;
                }
            }
            Tool t = Tool.of(shape, origin, side, axis, normal, scale, grounded ? new double[] { x, z } : null);
            return t.maxY() > Structures.CEILING || t.minY() < 0 ? null : t;
        }
    }.worn(Structures.WORN, 6);

    public final ToolShape shape;
    public final double scale;
    private final double[] origin, side, axis, normal, bounds;
    /** Where it is driven into the ground, as {x, z}, or null when it floats. */
    private final double[] foot;

    private Tool(ToolShape shape, double[] origin, double[] side, double[] axis, double[] normal, double scale,
        double[] foot, double[] centre, double footprint, double minY, double maxY) {
        super(centre[0], centre[1], footprint, minY, maxY);
        this.shape = shape;
        this.origin = origin;
        this.side = side;
        this.axis = axis;
        this.normal = normal;
        this.scale = scale;
        this.foot = foot;
        this.bounds = shape.bounds();
    }

    static Tool of(ToolShape shape, double[] origin, double[] side, double[] axis, double[] normal, double scale,
        double[] foot) {
        double[] b = shape.bounds();
        double[][] corners = new double[8][];
        int k = 0;
        for (double u : new double[] { b[0], b[1] }) {
            for (double v : new double[] { b[2], b[3] }) {
                for (double w : new double[] { b[4], b[5] }) {
                    corners[k++] = at(origin, side, axis, normal, scale, u, v, w);
                }
            }
        }
        double cx = 0, cz = 0, low = Double.MAX_VALUE, high = -Double.MAX_VALUE;
        for (double[] c : corners) {
            cx += c[0] / corners.length;
            cz += c[2] / corners.length;
            low = Math.min(low, c[1]);
            high = Math.max(high, c[1]);
        }
        double reach = 0;
        for (double[] c : corners) reach = Math.max(reach, Math.hypot(c[0] - cx, c[2] - cz));
        return new Tool(
            shape,
            origin,
            side,
            axis,
            normal,
            scale,
            foot,
            new double[] { cx, cz },
            reach + 1,
            low - 1,
            high + 1);
    }

    /** {lowest, highest} height of the box, less that of the local point mid. */
    private double[] extent(double[] mid) {
        double y = world(mid[0], mid[1], mid[2])[1];
        return new double[] { minY() + 1 - y, maxY() - 1 - y };
    }

    private static double[] at(double[] origin, double[] side, double[] axis, double[] normal, double scale, double u,
        double v, double w) {
        double[] p = new double[3];
        for (int i = 0; i < 3; i++) p[i] = origin[i] + (side[i] * u + axis[i] * v + normal[i] * w) * scale;
        return p;
    }

    /** World {x, y, z} of a point in the shape's local frame, in pixels. */
    public double[] world(double u, double v, double w) {
        return at(origin, side, axis, normal, scale, u, v, w);
    }

    @Override
    public double[][] feet() {
        return foot == null ? new double[0][] : new double[][] { foot };
    }

    @Override
    protected double body(double x, double y, double z) {
        double px = x - origin[0], py = y - origin[1], pz = z - origin[2];
        double u = (px * side[0] + py * side[1] + pz * side[2]) / scale;
        double v = (px * axis[0] + py * axis[1] + pz * axis[2]) / scale;
        double w = (px * normal[0] + py * normal[1] + pz * normal[2]) / scale;
        if (u < bounds[0] || u > bounds[1] || v < bounds[2] || v > bounds[3] || w < bounds[4] || w > bounds[5])
            return -1;
        return shape.inside(u, v, w) ? 1 : -1;
    }

    private static double[] scaled(double[] a, double k) {
        return new double[] { a[0] * k, a[1] * k, a[2] * k };
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] { a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0] };
    }

    private static double[] normalize(double[] a) {
        double l = Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
        return new double[] { a[0] / l, a[1] / l, a[2] / l };
    }
}
