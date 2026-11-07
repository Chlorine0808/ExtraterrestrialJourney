package chlorine.etjourney.world.end.feature.cloud;

import chlorine.etjourney.world.end.feature.Structure;
import chlorine.etjourney.world.end.feature.StructureProbe;
import chlorine.etjourney.world.end.noise.Distance;
import chlorine.etjourney.world.end.noise.Hash;

/** ANVILS: a stalk rising from the ground or the void under a flat cap that drifts to one side. */
public final class Anvil extends Structure {

    /** Largest radius the cap's rim is rounded off by; the top stays level, so it shows no terraces. */
    private static final double RIM_ROUND = 6;
    /** Farthest the ruffles push the stalk's side out. */
    private static final double STALK_RUFFLE = 1.5;

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
            double thick = 6 + 12 * Hash.hash01(s + 4, cx, cz);
            double capY = Math.min(Clouds.CEILING - thick - 1, 150 + 80 * Hash.hash01(s + 5, cx, cz));
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
        super(x, z, drift + radius * stretch * 1.15 + 1, base - 1, capY + thick + 1);
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
        return new Plumb(x, z).body(y);
    }

    @Override
    protected Line bodyLine(double x, double z) {
        Plumb p = new Plumb(x, z);
        // Widened by half at most and ruffled by STALK_RUFFLE, the stalk reaches no further out than this.
        boolean stem = p.d <= stalk * 1.5 + STALK_RUFFLE + 1e-6;
        boolean cap = p.rim - p.dc >= 0;
        if (!stem && !cap) return Line.EMPTY;
        double low = cap ? p.floor : Double.POSITIVE_INFINITY, high = cap ? p.roof : Double.NEGATIVE_INFINITY;
        if (stem) {
            low = Math.min(low, base);
            high = Math.max(high, capY + 2);
        }
        return Line.of(Math.max(low, minY()), Math.min(high, maxY()), stem ? y -> p.body(y) >= 0 : y -> p.cap(y) >= 0);
    }

    /** The terms of one vertical line that do not change with height. */
    private final class Plumb {

        final double x, z, d, dc, rim, roof, floor;

        Plumb(double x, double z) {
            this.x = x;
            this.z = z;
            d = Distance.planar(x - centreX, z - centreZ);
            double px = x - capX, pz = z - capZ, c = Math.cos(heading), s = Math.sin(heading);
            dc = Distance.planar((px * c + pz * s) / stretch, -px * s + pz * c);
            rim = radius + Clouds.ruffle(salt + 2, x, 0, z, 16, 0.15 * radius);
            roof = capY + thick;
            floor = capY + Clouds.ruffle(salt + 6, x, 0, z, 12, 1.5);
        }

        double body(double y) {
            // The stalk widens by half towards the cap and runs two blocks into it.
            double widen = 1 + 0.5 * Math.max(0, Math.min(1, (y - base) / (capY - base)));
            double side = stalk * widen - d + Clouds.ruffle(salt, x, y, z, 6, STALK_RUFFLE);
            double stem = Math.min(side, Math.min(y - base, capY + 2 - y));
            return Math.max(stem, cap(y));
        }

        double cap(double y) {
            // A slab between floor and roof whose rim is rounded off in the vertical section through the cap centre.
            double half = (roof - floor) / 2, round = Math.min(half, RIM_ROUND);
            double qx = dc - (rim - round), qy = Math.abs(y - (roof + floor) / 2) - (half - round);
            return round - Distance.planar(Math.max(qx, 0), Math.max(qy, 0)) - Math.min(Math.max(qx, qy), 0);
        }
    }
}
