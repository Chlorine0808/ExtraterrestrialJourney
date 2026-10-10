package chlorine.etjourney.world.end.feature.cloud;

import chlorine.etjourney.world.end.feature.Structure;
import chlorine.etjourney.world.end.feature.StructureProbe;
import chlorine.etjourney.world.end.noise.Hash;

/** ANVILS: a stalk rising from the ground or the void under a flat cap that drifts to one side. */
public final class Anvil extends Structure {

    /** Height the cap's top rises above its rim. */
    private static final double DOME = 4;

    public static final Structure.Kind<Anvil> KIND = new Structure.Kind<Anvil>("ANVILS", 160, 101) {

        @Override
        protected Anvil compute(long seed, int cx, int cz, StructureProbe probe) {
            long s = seed ^ 0x7D2E5A9C3F1B6048L;
            if (Hash.hash01(s, cx, cz) > 0.5) return null;
            double x = Clouds.place(cx, cell, 0.25, Hash.hash01(s + 1, cx, cz));
            double z = Clouds.place(cz, cell, 0.25, Hash.hash01(s + 2, cx, cz));
            if (!Clouds.forms(s, cx, cz, probe.weight(style, x, z))) return null;
            double ground = probe.ground(x, z);
            double base = ground > -100 ? ground - 4 : 20 + 40 * Hash.hash01(s + 3, cx, cz);
            double thick = 8 + 4 * Hash.hash01(s + 4, cx, cz);
            double capY = Math.min(Clouds.CEILING - thick - DOME - 1, 150 + 80 * Hash.hash01(s + 5, cx, cz));
            if (capY - base < 60) return null;
            double stalk = 8 + 4 * Hash.hash01(s + 6, cx, cz);
            double radius = 40 + 30 * Hash.hash01(s + 7, cx, cz);
            double drift = 15 + 15 * Hash.hash01(s + 8, cx, cz);
            double angle = Hash.hash01(s + 9, cx, cz) * Math.PI * 2;
            return new Anvil(
                x,
                z,
                base,
                stalk,
                x + Math.cos(angle) * drift,
                z + Math.sin(angle) * drift,
                capY,
                radius,
                thick,
                drift);
        }
    };

    public final double base, capY, capX, capZ, radius, thick;
    private final double stalk;

    Anvil(double x, double z, double base, double stalk, double capX, double capZ, double capY, double radius,
        double thick, double drift) {
        super(x, z, drift + radius + 1, base - 1, capY + thick + DOME + 1);
        this.base = base;
        this.stalk = stalk;
        this.capX = capX;
        this.capZ = capZ;
        this.capY = capY;
        this.radius = radius;
        this.thick = thick;
    }

    @Override
    protected double body(double x, double y, double z) {
        // The stalk widens by half towards the cap and runs two blocks into it.
        double widen = 1 + 0.5 * Math.max(0, Math.min(1, (y - base) / (capY - base)));
        double stem = Math.min(stalk * widen - Math.hypot(x - centreX, z - centreZ), Math.min(y - base, capY + 2 - y));
        double dc = Math.hypot(x - capX, z - capZ), u = Math.min(1, dc / radius);
        double roof = capY + thick + DOME * (1 - u * u);
        double cap = Math.min(radius - dc, Math.min(y - capY, roof - y));
        return Math.max(stem, cap);
    }
}
