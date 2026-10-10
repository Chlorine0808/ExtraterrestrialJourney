package chlorine.etjourney.world.end.feature.cloud;

import chlorine.etjourney.world.end.feature.Structure;
import chlorine.etjourney.world.end.feature.StructureProbe;
import chlorine.etjourney.world.end.noise.Hash;

/** LENTICULARS: three to five lens-shaped discs stacked with gaps, drawn block by block for their thin rims. */
public final class Lenticular extends Structure {

    public static final Structure.Kind<Lenticular> KIND = new Structure.Kind<Lenticular>("LENTICULARS", 112, 41) {

        @Override
        protected Lenticular compute(long seed, int cx, int cz, StructureProbe probe) {
            long s = seed ^ 0x5E3A8C1F9D2B7064L;
            if (Hash.hash01(s, cx, cz) > 0.5) return null;
            double x = Clouds.place(cx, cell, 0.25, Hash.hash01(s + 1, cx, cz));
            double z = Clouds.place(cz, cell, 0.25, Hash.hash01(s + 2, cx, cz));
            if (!Clouds.forms(s, cx, cz, probe.weight(style, x, z))) return null;
            int discs = 3 + (int) (2.999 * Hash.hash01(s + 3, cx, cz));
            double radius = 18 + 22 * Hash.hash01(s + 4, cx, cz);
            double gap = 4 + 4 * Hash.hash01(s + 5, cx, cz);
            double thick = 3 + 3 * Hash.hash01(s + 6, cx, cz);
            double height = discs * (thick + gap);
            double y = Clouds
                .between(Math.max(80, Clouds.floor(probe, x, z)), 220 - height, Hash.hash01(s + 7, cx, cz));
            return Double.isNaN(y) ? null : new Lenticular(x, y, z, discs, radius, gap, thick);
        }
    };

    public final double bottom, radius, gap, thick;
    public final int discs;

    Lenticular(double x, double bottom, double z, int discs, double radius, double gap, double thick) {
        super(x, z, radius + 1, bottom - 1, bottom + discs * (thick + gap) + 1);
        this.bottom = bottom;
        this.discs = discs;
        this.radius = radius;
        this.gap = gap;
        this.thick = thick;
    }

    /** Height of the middle of disc i, counted from the bottom one. */
    public double discCentre(int i) {
        return bottom + i * (thick + gap) + thick / 2;
    }

    @Override
    protected double body(double x, double y, double z) {
        double r = Math.hypot(x - centreX, z - centreZ), best = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < discs; i++) {
            // Upper discs are a little smaller and thinner.
            double rim = radius * (1 - 0.12 * i);
            if (r >= rim) continue;
            double u = r / rim, half = thick * (1 - 0.1 * i) / 2 * (1 - u * u);
            best = Math.max(best, half - Math.abs(y - discCentre(i)));
        }
        return best;
    }
}
