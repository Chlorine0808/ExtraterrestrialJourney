package chlorine.etjourney.world.end.feature.cloud;

import chlorine.etjourney.world.end.feature.Structure;
import chlorine.etjourney.world.end.feature.StructureProbe;
import chlorine.etjourney.world.end.noise.Hash;

/** ANVILS: a stalk rising from the ground or the void under a flat cap that drifts to one side. */
public final class Anvil extends Structure {

    /** Height the cap's top rises above its rim. */
    private static final double DOME = 4;
    /** Farthest the lumps lift the roof. */
    private static final double LUMPS = 2;

    public static final Structure.Kind<Anvil> KIND = new Structure.Kind<Anvil>("ANVILS", 160, 145) {

        @Override
        protected Anvil compute(long seed, int cx, int cz, StructureProbe probe) {
            long s = seed ^ 0x7D2E5A9C3F1B6048L;
            if (Hash.hash01(s, cx, cz) > 0.5) return null;
            double x = Clouds.place(cx, cell, 0.25, Hash.hash01(s + 1, cx, cz));
            double z = Clouds.place(cz, cell, 0.25, Hash.hash01(s + 2, cx, cz));
            if (!Clouds.forms(s, cx, cz, probe.weight(style, x, z))) return null;
            double ground = probe.ground(x, z);
            double base = ground > -100 ? ground - 4 : 20 + 40 * Hash.hash01(s + 3, cx, cz);
            double thick = 4 + 10 * Hash.hash01(s + 4, cx, cz);
            double capY = Math.min(Clouds.CEILING - thick - DOME - LUMPS - 1, 150 + 80 * Hash.hash01(s + 5, cx, cz));
            if (capY - base < 60) return null;
            double stalk = 8 + 4 * Hash.hash01(s + 6, cx, cz);
            double radius = 25 + 40 * Hash.hash01(s + 7, cx, cz);
            double drift = 15 + 15 * Hash.hash01(s + 8, cx, cz);
            double angle = Hash.hash01(s + 9, cx, cz) * Math.PI * 2;
            // The cap spreads further downwind.
            double stretch = 1 + 0.5 * Hash.hash01(s + 10, cx, cz);
            return new Anvil(x, z, base, stalk, capY, radius, thick, drift, angle, stretch, s ^ ((long) cx << 20) ^ cz);
        }
    };

    public final double base, capY, capX, capZ, radius, thick, heading, stretch;
    private final double stalk;
    private final long salt;

    Anvil(double x, double z, double base, double stalk, double capY, double radius, double thick, double drift,
        double heading, double stretch, long salt) {
        super(x, z, drift + radius * stretch * 1.15 + 1, base - 1, capY + thick + DOME + LUMPS + 1);
        this.base = base;
        this.stalk = stalk;
        this.capX = x + Math.cos(heading) * drift;
        this.capZ = z + Math.sin(heading) * drift;
        this.capY = capY;
        this.radius = radius;
        this.thick = thick;
        this.heading = heading;
        this.stretch = stretch;
        this.salt = salt;
    }

    @Override
    protected double body(double x, double y, double z) {
        // The stalk widens by half towards the cap and runs two blocks into it.
        double widen = 1 + 0.5 * Math.max(0, Math.min(1, (y - base) / (capY - base)));
        double side = stalk * widen - Math.hypot(x - centreX, z - centreZ) + Clouds.ruffle(salt, x, y, z, 6, 1.5);
        double stem = Math.min(side, Math.min(y - base, capY + 2 - y));
        double px = x - capX, pz = z - capZ, c = Math.cos(heading), s = Math.sin(heading);
        double dc = Math.hypot((px * c + pz * s) / stretch, -px * s + pz * c), u = Math.min(1, dc / radius);
        double rim = radius + Clouds.ruffle(salt + 2, x, 0, z, 16, 0.15 * radius);
        double roof = capY + thick + DOME * (1 - u * u) + Clouds.ruffle(salt + 4, x, 0, z, 10, LUMPS);
        double floor = capY + Clouds.ruffle(salt + 6, x, 0, z, 12, 1.5);
        double cap = Math.min(rim - dc, Math.min(y - floor, roof - y));
        return Math.max(stem, cap);
    }
}
