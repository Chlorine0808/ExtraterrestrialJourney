package chlorine.etjourney.world.end.feature;

import java.util.ArrayList;
import java.util.List;

import chlorine.etjourney.world.end.noise.Hash;

/** A cluster of thin needles tapering to a point, standing on the land of SPIRES regions; drawn block by block. */
public final class Spire extends Structure {

    /** How far each needle's root sinks below the lowest ground around it. */
    private static final double SINK = 3;

    public static final Structure.Kind<Spire> KIND = new Structure.Kind<Spire>("SPIRES", 16, 16) {

        @Override
        protected Spire compute(long seed, int cx, int cz, StructureProbe probe) {
            long s = seed ^ 0x1F6B3D8A5C7E2049L;
            if (Hash.hash01(s, cx, cz) > 0.85) return null;
            double x = (cx + 0.2 + 0.6 * Hash.hash01(s + 1, cx, cz)) * cell;
            double z = (cz + 0.2 + 0.6 * Hash.hash01(s + 2, cx, cz)) * cell;
            if (!Structures.forms(s, cx, cz, probe.weight(style, x, z))) return null;
            int n = 1 + (int) (5 * Hash.hash01(s + 3, cx, cz));
            long k = s + 31L * cx + cz;
            List<Needle> needles = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                double angle = Hash.hash01(k, i, 1) * Math.PI * 2;
                double out = i == 0 ? 0 : 2 + 4 * Hash.hash01(k, i, 2);
                double fx = x + Math.cos(angle) * out, fz = z + Math.sin(angle) * out;
                double radius = 1.2 + 1.8 * Hash.hash01(k, i, 3);
                double ground = lowestGround(probe, fx, fz, radius + 1);
                if (ground <= -100) continue;
                // Mostly short and slim, now and then a tall one.
                double roll = Hash.hash01(k, i, 4);
                double height = roll < 0.85 ? 10 + 30 * roll / 0.85 : 45 + 30 * (roll - 0.85) / 0.15;
                height *= 0.6 + 0.4 * radius / 3;
                double y0 = ground - SINK, y1 = Math.min(Structures.CEILING, ground + height);
                if (y1 - ground < 6) continue;
                // A slight lean, outwards from the cluster for the outer needles.
                double lean = (y1 - y0) * 0.12 * Hash.hash01(k, i, 5);
                double leanAngle = i == 0 ? Hash.hash01(k, i, 6) * Math.PI * 2 : angle;
                needles.add(
                    new Needle(
                        fx,
                        y0,
                        fz,
                        fx + Math.cos(leanAngle) * lean,
                        y1,
                        fz + Math.sin(leanAngle) * lean,
                        radius));
            }
            return needles.isEmpty() ? null : new Spire(x, z, needles.toArray(new Needle[0]));
        }
    };

    /** The lowest ground under a needle's foot, so a foot on a slope is buried on its downhill side too. */
    private static double lowestGround(StructureProbe probe, double x, double z, double reach) {
        double low = probe.ground(x, z);
        for (int a = 0; a < 4; a++) {
            double angle = a * Math.PI / 2;
            low = Math.min(low, probe.ground(x + Math.cos(angle) * reach, z + Math.sin(angle) * reach));
        }
        return low;
    }

    /** One needle from its root (x0, y0, z0) to its tip (x1, y1, z1), radius at the root. */
    public static final class Needle {

        final double x0, y0, z0, x1, y1, z1, radius;

        Needle(double x0, double y0, double z0, double x1, double y1, double z1, double radius) {
            this.x0 = x0;
            this.y0 = y0;
            this.z0 = z0;
            this.x1 = x1;
            this.y1 = y1;
            this.z1 = z1;
            this.radius = radius;
        }

        /** The axis point at height y, as {x, y, z}. */
        double[] axisAt(double y) {
            double f = (y - y0) / (y1 - y0);
            return new double[] { x0 + (x1 - x0) * f, y, z0 + (z1 - z0) * f };
        }

        double density(double x, double y, double z) {
            double f = (y - y0) / (y1 - y0);
            if (f >= 1) return y1 - y;
            double fc = Math.max(0, f);
            // Concave taper: the needle thins fast and ends in a long fine point.
            double r = radius * Math.pow(1 - fc, 1.4);
            double d = r - Math.hypot(x - (x0 + (x1 - x0) * fc), z - (z0 + (z1 - z0) * fc));
            return Math.min(d, y - y0 + 1);
        }

        double reach(double centreX, double centreZ) {
            return Math.max(Math.hypot(x0 - centreX, z0 - centreZ), Math.hypot(x1 - centreX, z1 - centreZ)) + radius;
        }
    }

    final Needle[] needles;

    Spire(double x, double z, Needle[] needles) {
        super(x, z, footprint(x, z, needles), bottom(needles) - 1, top(needles) + 1);
        this.needles = needles;
    }

    private static double footprint(double x, double z, Needle[] needles) {
        double reach = 0;
        for (Needle n : needles) reach = Math.max(reach, n.reach(x, z));
        return reach + 1;
    }

    private static double bottom(Needle[] needles) {
        double low = Double.MAX_VALUE;
        for (Needle n : needles) low = Math.min(low, n.y0);
        return low;
    }

    private static double top(Needle[] needles) {
        double top = 0;
        for (Needle n : needles) top = Math.max(top, n.y1);
        return top;
    }

    @Override
    public double[][] feet() {
        double[][] feet = new double[needles.length][];
        for (int i = 0; i < needles.length; i++) feet[i] = new double[] { needles[i].x0, needles[i].z0 };
        return feet;
    }

    /** The needles whose root is clear of every sinkhole, or null when none are; a hole leaves nothing to stand on. */
    public Spire outsideHoles(long seed, List<Holes.Hole> holes) {
        List<Needle> kept = new ArrayList<>();
        for (Needle n : needles) if (Holes.cutFraction(seed, holes, n.x0, n.z0) <= 0) kept.add(n);
        if (kept.size() == needles.length) return this;
        return kept.isEmpty() ? null : new Spire(centreX, centreZ, kept.toArray(new Needle[0]));
    }

    @Override
    protected double body(double x, double y, double z) {
        double d = Double.NEGATIVE_INFINITY;
        for (Needle n : needles) d = Math.max(d, n.density(x, y, z));
        return d;
    }
}
