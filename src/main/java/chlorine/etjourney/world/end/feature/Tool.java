package chlorine.etjourney.world.end.feature;

import chlorine.etjourney.world.end.noise.Hash;

/**
 * A giant tool in CROSSES regions: a tool sprite blown up to 2-5 blocks a pixel and, like a cross's beams, as thick as
 * it is wide. Over land most are driven in at a lean by their planted end; the rest float at any angle. Drawn block by
 * block.
 */
public final class Tool extends Structure {

    private static final double ROOT2 = Math.sqrt(2);

    public static final Structure.Kind<Tool> KIND = new Structure.Kind<Tool>("CROSSES", 112, 64) {

        @Override
        protected Tool compute(long seed, int cx, int cz, StructureProbe probe) {
            long s = seed ^ 0x2E6C9A1F5B3D7048L;
            if (Hash.hash01(s, cx, cz) > 0.5) return null;
            double x = (cx + 0.2 + 0.6 * Hash.hash01(s + 1, cx, cz)) * cell;
            double z = (cz + 0.2 + 0.6 * Hash.hash01(s + 2, cx, cz)) * cell;
            if (!Structures.forms(s, cx, cz, probe.weight(style, x, z))) return null;
            ToolSprite[] sprites = ToolSprite.values();
            ToolSprite sprite = sprites[(int) (Hash.hash01(s + 3, cx, cz) * sprites.length)];
            // More small tools than large ones.
            double scale = 2 + 3 * Math.pow(Hash.hash01(s + 4, cx, cz), 1.5);
            double depth = ToolSprite.MAX_THICKNESS * scale;
            double ground = probe.ground(x, z);
            boolean grounded = ground > -100 && Hash.hash01(s + 5, cx, cz) < 0.6;
            double lean = Math.toRadians((grounded ? 25 : 70) * Hash.hash01(s + 6, cx, cz));
            double yaw = Hash.hash01(s + 7, cx, cz) * Math.PI * 2, twist = Hash.hash01(s + 8, cx, cz) * Math.PI * 2;
            double[] up = { Math.sin(lean) * Math.cos(yaw), Math.cos(lean), Math.sin(lean) * Math.sin(yaw) };
            double[] side = normalize(cross(up, new double[] { Math.cos(twist), 0, Math.sin(twist) }));
            // The sprite's diagonal runs from the handle to the head; a tool driven in tip first hangs it downwards.
            double[] diagonal = sprite.tipFirst ? scaled(up, -1) : up;
            double[] e1 = scaled(add(diagonal, side), 1 / ROOT2),
                e2 = scaled(add(diagonal, scaled(side, -1)), 1 / ROOT2);
            double[] origin;
            if (grounded) {
                // An eighth of the diagonal buried below the planted end.
                int[] planted = sprite.planted();
                double pu = (planted[0] + 0.5) * scale, pv = (15.5 - planted[1]) * scale;
                double buried = 16 * ROOT2 * scale * 0.12;
                double[] foot = { x, ground, z };
                origin = new double[3];
                for (int i = 0; i < 3; i++) origin[i] = foot[i] - up[i] * buried - e1[i] * pu - e2[i] * pv;
            } else {
                double[] around = extent(e1, e2, scale, depth);
                double low = (ground > -100 ? ground + 6 : Structures.VOID_BASE) - around[0];
                double high = Structures.CEILING - around[1];
                if (low > high) return null;
                double y = low + Math.min(high - low, 20 + 120 * Hash.hash01(s + 9, cx, cz));
                origin = new double[] { x - (e1[0] + e2[0]) * 8 * scale, y - (e1[1] + e2[1]) * 8 * scale,
                    z - (e1[2] + e2[2]) * 8 * scale };
            }
            Tool t = Tool
                .of(sprite, origin, e1, e2, cross(e1, e2), scale, depth, grounded ? new double[] { x, z } : null);
            return t.maxY() > Structures.CEILING || t.minY() < 0 ? null : t;
        }
    }.worn(Structures.WORN, 6);

    public final ToolSprite sprite;
    /** Blocks a pixel, and the thickest the tool gets in blocks. */
    public final double scale, depth;
    private final double[] origin, e1, e2, normal;
    /** Where it is driven into the ground, as {x, z}, or null when it floats. */
    private final double[] foot;

    private Tool(ToolSprite sprite, double[] origin, double[] e1, double[] e2, double[] normal, double scale,
        double depth, double[] foot, double[] centre, double footprint, double minY, double maxY) {
        super(centre[0], centre[1], footprint, minY, maxY);
        this.sprite = sprite;
        this.origin = origin;
        this.e1 = e1;
        this.e2 = e2;
        this.normal = normal;
        this.scale = scale;
        this.depth = depth;
        this.foot = foot;
    }

    static Tool of(ToolSprite sprite, double[] origin, double[] e1, double[] e2, double[] normal, double scale,
        double depth, double[] foot) {
        double[][] corners = corners(origin, e1, e2, normal, scale, depth);
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
            sprite,
            origin,
            e1,
            e2,
            normal,
            scale,
            depth,
            foot,
            new double[] { cx, cz },
            reach + 1,
            low - 1,
            high + 1);
    }

    /** {lowest, highest} offset of the sprite's box from its centre, for a frame and size. */
    static double[] extent(double[] e1, double[] e2, double scale, double depth) {
        double[] origin = { -(e1[0] + e2[0]) * 8 * scale, -(e1[1] + e2[1]) * 8 * scale, -(e1[2] + e2[2]) * 8 * scale };
        double low = Double.MAX_VALUE, high = -Double.MAX_VALUE;
        for (double[] c : corners(origin, e1, e2, cross(e1, e2), scale, depth)) {
            low = Math.min(low, c[1]);
            high = Math.max(high, c[1]);
        }
        return new double[] { low, high };
    }

    private static double[][] corners(double[] origin, double[] e1, double[] e2, double[] normal, double scale,
        double depth) {
        double[][] out = new double[8][];
        int k = 0;
        for (double u : new double[] { 0, 16 * scale }) {
            for (double v : new double[] { 0, 16 * scale }) {
                for (double w : new double[] { -depth / 2, depth / 2 }) {
                    out[k++] = new double[] { origin[0] + e1[0] * u + e2[0] * v + normal[0] * w,
                        origin[1] + e1[1] * u + e2[1] * v + normal[1] * w,
                        origin[2] + e1[2] * u + e2[2] * v + normal[2] * w };
                }
            }
        }
        return out;
    }

    /** World {x, y, z} of the middle of a sprite pixel, w blocks off its face. */
    public double[] pixel(int col, int row, double w) {
        double u = (col + 0.5) * scale, v = (15.5 - row) * scale;
        return new double[] { origin[0] + e1[0] * u + e2[0] * v + normal[0] * w,
            origin[1] + e1[1] * u + e2[1] * v + normal[1] * w, origin[2] + e1[2] * u + e2[2] * v + normal[2] * w };
    }

    @Override
    public double[][] feet() {
        return foot == null ? new double[0][] : new double[][] { foot };
    }

    @Override
    protected double body(double x, double y, double z) {
        double px = x - origin[0], py = y - origin[1], pz = z - origin[2];
        double w = px * normal[0] + py * normal[1] + pz * normal[2];
        if (Math.abs(w) > depth / 2) return -1;
        double u = px * e1[0] + py * e1[1] + pz * e1[2], v = px * e2[0] + py * e2[1] + pz * e2[2];
        int col = (int) Math.floor(u / scale), row = 15 - (int) Math.floor(v / scale);
        return Math.abs(w) < sprite.thickness(col, row) * scale / 2 ? 1 : -1;
    }

    private static double[] add(double[] a, double[] b) {
        return new double[] { a[0] + b[0], a[1] + b[1], a[2] + b[2] };
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
