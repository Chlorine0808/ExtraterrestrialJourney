package chlorine.etjourney.world.end.feature;

/**
 * Solid shapes of the giant tools, built from parts as the crosses are. Measured in pixels in a local frame: y runs up
 * the handle from its planted end, x across in the tool's plane and z through it.
 */
public enum ToolShape {

    PICKAXE(false, 0, 0, new double[] { -10.5, 10.5, -0.25, 22.25, -2, 2 }) {

        @Override
        public boolean inside(double x, double y, double z) {
            return rod(x, y, z, 0, 19.5, 0.9) || arcBar(x, y, z, 6, 14, 0.8, 1.7, 0.35, 0)
                || roundBox(x, y, z, 0, 19.8, 0, 1.5, 2.2, 1.9, 0.5);
        }
    },
    AXE(false, 0, 0, new double[] { -2.5, 9.25, -0.25, 22.5, -1.75, 1.75 }) {

        @Override
        public boolean inside(double x, double y, double z) {
            return rod(x, y, z, 0, 20.5, 0.9) || roundBox(x, y, z, -0.3, 17.5, 0, 2, 3, 1.6, 0.5)
                || wedge(x, y, z, 1.5, 7.5, 17, 2.6, 5.5, 1.4);
        }
    },
    HOE(false, 0, 0, new double[] { -1.75, 10.5, -0.25, 22.25, -2, 2 }) {

        @Override
        public boolean inside(double x, double y, double z) {
            // Half a pickaxe head: one curved blade thinning to its tip.
            return rod(x, y, z, 0, 19.5, 0.9) || arcBar(x, y, z, 6, 14, 0.8, 1.7, 0.35, 1)
                || roundBox(x, y, z, 0, 19.8, 0, 1.5, 2.2, 1.9, 0.5);
        }
    },
    SWORD(true, 0, 21.3, new double[] { -5.25, 5.25, -0.5, 21.75, -1.5, 1.5 }) {

        @Override
        public boolean inside(double x, double y, double z) {
            // A blade of diamond section narrowing to a point over its last fifth.
            double t = (y - 6.2) / 15.5;
            if (t >= 0 && t <= 1) {
                double w = 1.6 * (t > 0.82 ? (1 - t) / 0.18 : 1);
                double ridge = 0.75 * Math.max(w / 1.6, 0.3);
                if (w > 0 && Math.abs(x) / w + Math.abs(z) / ridge <= 1) return true;
            }
            return roundBox(x, y, z, 0, 5.6, 0, 4.2, 0.65, 1.1, 0.4) || ball(x, y, z, 4.2, 5.6, 0, 0.9)
                || ball(x, y, z, -4.2, 5.6, 0, 0.9)
                || rod(x, y, z, 1.2, 5, 0.75)
                || ball(x, y, z, 0, 1, 0, 1.25);
        }
    },
    SHOVEL(false, 0, 0, new double[] { -3.25, 3.25, -0.25, 28.25, -1.5, 1.5 }) {

        @Override
        public boolean inside(double x, double y, double z) {
            // A dished blade, square at the collar and rounded at the tip.
            double up = y - 15.5;
            double halfWidth = up < 4.5 ? 3.2 : 3.2 * Math.sqrt(Math.max(0, 1 - sq((up - 4.5) / 3.2)));
            boolean blade = up >= 0 && Math.abs(x) <= halfWidth
                && Math.abs(z - 0.09 * x * x + 0.4) <= 0.35 + 0.25 * (1 - clamp(up / 7, 0, 1));
            return rod(x, y, z, 0, 16, 0.85) || blade
                || roundBox(x, y, z, 0, 15.2, 0, 1.4, 1.6, 1.4, 0.4)
                || roundBox(x, y, z, 0, 0.6, 0, 2.2, 0.6, 0.8, 0.4);
        }
    },
    BOW(false, -2, 1.6, new double[] { -2.5, 5, 0.75, 21.25, -1.5, 1.5 }) {

        @Override
        public boolean inside(double x, double y, double z) {
            // Limbs bent along a circle, thinning to their tips, with the string running between them.
            double cy = 11, radius = 11, span = 1.05;
            double dx = x + radius - 3.5, dy = y - cy;
            double rho = Math.sqrt(dx * dx + dy * dy), angle = Math.atan2(dy, dx);
            double r = 1.15 - 0.6 * clamp(Math.abs(angle) / span, 0, 1);
            if (Math.abs(angle) <= span && sq(rho - radius) + z * z <= r * r) return true;
            if (Math.abs(dy) <= 2 && sq(rho - radius) + z * z <= 1.45 * 1.45) return true;
            double tipX = 3.5 - radius + radius * Math.cos(span);
            return sq(x - tipX) + z * z <= 0.45 * 0.45 && Math.abs(dy) <= radius * Math.sin(span);
        }
    },
    HAMMER(false, 0, 0, new double[] { -5.25, 6.25, -0.25, 22.25, -2.75, 2.75 }) {

        @Override
        public boolean inside(double x, double y, double z) {
            return rod(x, y, z, 0, 19, 1) || roundBox(x, y, z, 0.6, 19.5, 0, 5.2, 2.4, 2.4, 0.7)
                || roundBox(x, y, z, 5.6, 19.5, 0, 0.6, 2.7, 2.7, 0.5)
                || roundBox(x, y, z, -4.4, 19.5, 0, 0.6, 2.7, 2.7, 0.5);
        }
    },
    PAXEL(false, 0, 0, new double[] { -11, 8.75, -0.25, 24.25, -2, 2 }) {

        @Override
        public boolean inside(double x, double y, double z) {
            // A pick on one side, an axe blade on the other and a small spade on top.
            double spadeHalf = 2.2 * Math.sqrt(Math.max(0, 1 - sq((y - 20.5) / 3.5)));
            boolean spade = y >= 20.5 && y <= 24
                && Math.abs(x) <= spadeHalf + 0.01
                && Math.abs(z - 0.08 * x * x) <= 0.4;
            return rod(x, y, z, 0, 20, 0.9) || arcBar(x, y, z, 6, 14, 0.85, 1.7, 0.35, -1)
                || wedge(x, y, z, 1.5, 7, 17.5, 2.4, 5, 1.4)
                || spade
                || roundBox(x, y, z, 0, 19.2, 0, 1.6, 2.4, 1.8, 0.5);
        }
    },
    WRENCH(false, 0, -0.1, new double[] { -3.25, 3.25, -1, 23.5, -1, 1 }) {

        @Override
        public boolean inside(double x, double y, double z) {
            // A flat bar with an open jaw at the top and a ring at the bottom.
            boolean jaw = roundBox(x, y, z, 0, 20.5, 0, 3, 2.8, 0.8, 0.4)
                && !(Math.abs(x) <= 1.1 && Math.abs(y - 22) <= 2);
            double ring = Math.sqrt(x * x + sq(y - 1.6));
            return roundBox(x, y, z, 0, 10.5, 0, 1.1, 8, 0.6, 0.3) || jaw
                || Math.abs(ring - 1.7) <= 0.7 && Math.abs(z) <= 0.75;
        }
    };

    /** Whether the tool is driven in tip first, as a sword is, rather than by its handle. */
    public final boolean tipFirst;
    private final double plantedX, plantedY;
    private final double[] bounds;

    ToolShape(boolean tipFirst, double plantedX, double plantedY, double[] bounds) {
        this.tipFirst = tipFirst;
        this.plantedX = plantedX;
        this.plantedY = plantedY;
        this.bounds = bounds;
    }

    public abstract boolean inside(double x, double y, double z);

    /** {x, y} of the end driven into the ground. */
    public double[] planted() {
        return new double[] { plantedX, plantedY };
    }

    /** {minX, maxX, minY, maxY, minZ, maxZ} enclosing the shape, as sampling it every quarter pixel finds. */
    public double[] bounds() {
        return bounds.clone();
    }

    /** A round handle along y. */
    private static boolean rod(double x, double y, double z, double y0, double y1, double r) {
        return y >= y0 && y <= y1 && x * x + z * z <= r * r;
    }

    /** A box of the given half sizes with its edges rounded off by r. */
    private static boolean roundBox(double x, double y, double z, double cx, double cy, double cz, double hx, double hy,
        double hz, double r) {
        double qx = Math.max(Math.abs(x - cx) - (hx - r), 0), qy = Math.max(Math.abs(y - cy) - (hy - r), 0),
            qz = Math.max(Math.abs(z - cz) - (hz - r), 0);
        return qx * qx + qy * qy + qz * qz <= r * r;
    }

    private static boolean ball(double x, double y, double z, double cx, double cy, double cz, double r) {
        return sq(x - cx) + sq(y - cy) + sq(z - cz) <= r * r;
    }

    /**
     * A bar bent along a circle about (0, cy) in the xy plane, square in section and thinning from half0 at the top to
     * half1 at its tips, span radians either side; side 1 or -1 keeps only that side.
     */
    private static boolean arcBar(double x, double y, double z, double cy, double radius, double span, double half0,
        double half1, int side) {
        double dy = y - cy, angle = Math.atan2(x, dy);
        if (Math.abs(angle) > span) return false;
        if (side != 0 && Math.signum(angle) != side && Math.abs(angle) >= 0.05) return false;
        double half = half0 + (half1 - half0) * Math.abs(angle) / span;
        return Math.abs(Math.sqrt(x * x + dy * dy) - radius) <= half && Math.abs(z) <= half;
    }

    /**
     * An axe blade leaving the socket at x0 towards +x: it flares from h0 to h1 tall about cy and thins from t0 to an
     * edge that bows out to reach.
     */
    private static boolean wedge(double x, double y, double z, double x0, double reach, double cy, double h0, double h1,
        double t0) {
        double u = x - x0, edge = reach - 0.05 * sq(y - cy);
        if (u < 0 || u > edge) return false;
        double t = clamp(u / Math.max(edge, 0.1), 0, 1);
        return Math.abs(y - cy) <= h0 + (h1 - h0) * Math.pow(t, 1.5) && Math.abs(z) <= t0 * (1 - t) + 0.25;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static double sq(double v) {
        return v * v;
    }
}
