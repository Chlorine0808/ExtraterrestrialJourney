package chlorine.etjourney.world.end.feature;

import chlorine.etjourney.world.end.noise.Hash;

/** A tuft of thin stalks with small caps, splaying out from one root like enoki; drawn block by block. */
public final class Enoki extends Structure {

    /** Rock knob at the root, so a tuft over the void has something to grow from. */
    private static final double KNOB = 4;

    public static final Structure.Kind<Enoki> KIND = new Structure.Kind<Enoki>("MUSHROOMS", 40, 24) {

        @Override
        protected Enoki compute(long seed, int cx, int cz, StructureProbe probe) {
            long s = seed ^ 0x3C8A5E1F7B2D9046L;
            if (Hash.hash01(s, cx, cz) > 0.5) return null;
            double x = (cx + 0.2 + 0.6 * Hash.hash01(s + 1, cx, cz)) * cell;
            double z = (cz + 0.2 + 0.6 * Hash.hash01(s + 2, cx, cz)) * cell;
            if (!Structures.forms(s, cx, cz, probe.weight(style, x, z))) return null;
            double root = Structures.base(probe, x, z, Hash.hash01(s + 3, cx, cz)) + 3;
            double tall = 15 + 25 * Hash.hash01(s + 4, cx, cz);
            if (root + tall + 3 > Structures.CEILING) return null;
            int n = 8 + (int) (12 * Hash.hash01(s + 5, cx, cz));
            Stalk[] stalks = new Stalk[n];
            long k = s + 31L * cx + cz;
            for (int i = 0; i < n; i++) {
                double angle = Hash.hash01(k, i, 1) * Math.PI * 2;
                double start = 2.5 * Hash.hash01(k, i, 2), height = tall * (0.6 + 0.4 * Hash.hash01(k, i, 3));
                double spread = start + height * (0.1 + 0.25 * Hash.hash01(k, i, 4));
                double c = Math.cos(angle), sn = Math.sin(angle);
                stalks[i] = new Stalk(
                    x + c * start,
                    root,
                    z + sn * start,
                    x + c * spread,
                    root + height,
                    z + sn * spread,
                    0.9 + 0.5 * Hash.hash01(k, i, 5),
                    1.8 + 1.2 * Hash.hash01(k, i, 6),
                    1.2 + 0.8 * Hash.hash01(k, i, 7));
            }
            return new Enoki(x, z, root, stalks);
        }
    };

    /** One stalk from (x0, y0, z0) to (x1, y1, z1), topped by a flat cap. */
    public static final class Stalk {

        final double x0, y0, z0, x1, y1, z1, radius, capRadius, capHeight;

        Stalk(double x0, double y0, double z0, double x1, double y1, double z1, double radius, double capRadius,
            double capHeight) {
            this.x0 = x0;
            this.y0 = y0;
            this.z0 = z0;
            this.x1 = x1;
            this.y1 = y1;
            this.z1 = z1;
            this.radius = radius;
            this.capRadius = capRadius;
            this.capHeight = capHeight;
        }

        double density(double x, double y, double z) {
            double ex = x1 - x0, ey = y1 - y0, ez = z1 - z0;
            double px = x - x0, py = y - y0, pz = z - z0;
            double f = Math.max(0, Math.min(1, (px * ex + py * ey + pz * ez) / (ex * ex + ey * ey + ez * ez)));
            double stem = radius - Math.sqrt(sq(px - ex * f) + sq(py - ey * f) + sq(pz - ez * f));
            // The cap: a flattened ball sitting on the top of the stalk.
            double cx = (x - x1) / capRadius, cy = (y - y1 - capHeight * 0.5) / capHeight, cz = (z - z1) / capRadius;
            double cap = (1 - Math.sqrt(cx * cx + cy * cy + cz * cz)) * Math.min(capRadius, capHeight);
            return Math.max(stem, cap);
        }

        double reach(double centreX, double centreZ) {
            return Math.hypot(x1 - centreX, z1 - centreZ) + capRadius;
        }
    }

    final double root;
    final Stalk[] stalks;

    Enoki(double x, double z, double root, Stalk[] stalks) {
        super(x, z, footprint(x, z, stalks), root - KNOB - 1, top(stalks) + 1);
        this.root = root;
        this.stalks = stalks;
    }

    private static double footprint(double x, double z, Stalk[] stalks) {
        double reach = KNOB;
        for (Stalk s : stalks) reach = Math.max(reach, s.reach(x, z));
        return reach + 1;
    }

    private static double top(Stalk[] stalks) {
        double top = 0;
        for (Stalk s : stalks) top = Math.max(top, s.y1 + s.capHeight * 1.5);
        return top;
    }

    @Override
    protected double body(double x, double y, double z) {
        double d = KNOB - Math.sqrt(sq(x - centreX) + sq(y - root) + sq(z - centreZ));
        for (Stalk s : stalks) d = Math.max(d, s.density(x, y, z));
        return d;
    }

    private static double sq(double v) {
        return v * v;
    }
}
