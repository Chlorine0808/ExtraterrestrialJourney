package chlorine.etjourney.world.end.feature.cloud;

import chlorine.etjourney.world.end.feature.Structure;
import chlorine.etjourney.world.end.feature.StructureProbe;
import chlorine.etjourney.world.end.noise.Hash;

/** MAMMATUS: round pouches hanging from the underside of the land. */
public final class Mammatus extends Structure {

    /** Lowest underside a pouch hangs from, so it stays clear of the void floor. */
    private static final double MIN_UNDERSIDE = 40;

    public static final Structure.Kind<Mammatus> KIND = new Structure.Kind<Mammatus>("MAMMATUS", 24, 11) {

        @Override
        protected Mammatus compute(long seed, int cx, int cz, StructureProbe probe) {
            long s = seed ^ 0x4F8C2A6E1D3B9075L;
            if (Hash.hash01(s, cx, cz) > 0.7) return null;
            double x = Clouds.place(cx, cell, 0.25, Hash.hash01(s + 1, cx, cz));
            double z = Clouds.place(cz, cell, 0.25, Hash.hash01(s + 2, cx, cz));
            if (!Clouds.forms(s, cx, cz, probe.weight(style, x, z))) return null;
            double under = probe.underside(x, z);
            if (under < MIN_UNDERSIDE) return null;
            double r = 6 + 4 * Hash.hash01(s + 3, cx, cz);
            // The pouch rises 0.6 r into the slab; thinner land would show it above the ground.
            if (probe.ground(x, z) - under < 0.6 * r + 2) return null;
            // Sunk into the slab, so no gap opens between the pouch and the land.
            return new Mammatus(x, under - 0.4 * r, z, r);
        }
    };

    public final double centreY, radius;

    Mammatus(double x, double y, double z, double radius) {
        super(x, z, radius + 1, y - radius - 1, y + radius + 1);
        this.centreY = y;
        this.radius = radius;
    }

    @Override
    protected double body(double x, double y, double z) {
        double dx = x - centreX, dy = y - centreY, dz = z - centreZ;
        return radius - Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
