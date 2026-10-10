package chlorine.etjourney.world.end.feature;

import chlorine.etjourney.world.end.noise.Hash;

/** A solid round column among the hollow pillars, thin and tall or short and stubby; drawn block by block. */
public final class Column extends Structure {

    public static final Structure.Kind<Column> KIND = new Structure.Kind<Column>("HOLLOW_PILLARS", 48, 9) {

        @Override
        protected Column compute(long seed, int cx, int cz, StructureProbe probe) {
            long s = seed ^ 0x4D7B1E9A3C5F2086L;
            if (Hash.hash01(s, cx, cz) > 0.6) return null;
            double x = (cx + 0.2 + 0.6 * Hash.hash01(s + 1, cx, cz)) * cell;
            double z = (cz + 0.2 + 0.6 * Hash.hash01(s + 2, cx, cz)) * cell;
            if (!Structures.forms(s, cx, cz, probe.weight(style, x, z))) return null;
            double base = Structures.base(probe, x, z, Hash.hash01(s + 3, cx, cz));
            boolean thin = Hash.hash01(s + 4, cx, cz) < 0.5;
            double radius = thin ? 1.5 + 2 * Hash.hash01(s + 5, cx, cz) : 4 + 4 * Hash.hash01(s + 5, cx, cz);
            double rise = thin ? 30 + 80 * Hash.hash01(s + 6, cx, cz) : 6 + 14 * Hash.hash01(s + 6, cx, cz);
            double top = Math.min(Structures.CEILING, base + rise);
            if (top - base < (thin ? 30 : 6)) return null;
            return new Column(x, z, base, top, radius);
        }
    }.worn(Structures.WORN, 6);

    final double base, top, radius;

    Column(double x, double z, double base, double top, double radius) {
        super(x, z, radius + 1, base - 1, top + 1);
        this.base = base;
        this.top = top;
        this.radius = radius;
    }

    @Override
    protected double body(double x, double y, double z) {
        return Math.min(radius - Math.hypot(x - centreX, z - centreZ), Math.min(top - y, y - base));
    }
}
