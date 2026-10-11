package chlorine.etjourney.world.end.feature.cloud;

import chlorine.etjourney.world.end.feature.Structure;
import chlorine.etjourney.world.end.feature.StructureProbe;
import chlorine.etjourney.world.end.noise.Hash;

/** VIRGA: six to fifteen one-block streaks hanging from the underside, fraying away further down. */
public final class Virga extends Structure {

    public static final Structure.Kind<Virga> KIND = new Structure.Kind<Virga>("VIRGA", 32, 28) {

        @Override
        protected Virga compute(long seed, int cx, int cz, StructureProbe probe) {
            long s = seed ^ 0x6B1D4F8A2E5C3097L;
            if (Hash.hash01(s, cx, cz) > 0.6) return null;
            double x = Clouds.place(cx, cell, 0.25, Hash.hash01(s + 1, cx, cz));
            double z = Clouds.place(cz, cell, 0.25, Hash.hash01(s + 2, cx, cz));
            if (!Clouds.forms(s, cx, cz, probe.weight(style, x, z))) return null;
            double under = probe.underside(x, z);
            if (under < 40) return null;
            // The streaks start two blocks into the slab; thinner land would show them above the ground.
            if (probe.ground(x, z) - under < 4) return null;
            int count = 6 + (int) (9.999 * Hash.hash01(s + 3, cx, cz));
            double spread = 2 + 6 * Hash.hash01(s + 4, cx, cz);
            // Short and long falls, some blown aside as they drop.
            double scale = 0.4 + Hash.hash01(s + 5, cx, cz);
            double lean = Hash.hash01(s + 6, cx, cz) < 0.3 ? 0 : 0.25 * Hash.hash01(s + 7, cx, cz);
            double heading = Hash.hash01(s + 8, cx, cz) * Math.PI * 2;
            // Streaks as {x, z, length}.
            double[] strands = new double[count * 3];
            double longest = 0;
            for (int i = 0; i < count; i++) {
                double a = Hash.hash01(s + 10 + i, cx, cz) * Math.PI * 2;
                double d = spread * Math.sqrt(Hash.hash01(s + 30 + i, cx, cz));
                strands[i * 3] = x + Math.cos(a) * d;
                strands[i * 3 + 1] = z + Math.sin(a) * d;
                strands[i * 3 + 2] = (15 + 35 * Hash.hash01(s + 50 + i, cx, cz)) * scale;
                longest = Math.max(longest, strands[i * 3 + 2]);
            }
            // Starts inside the slab, so the streaks hang from it without a gap.
            return new Virga(
                x,
                z,
                Math.floor(under) + 2,
                strands,
                longest,
                spread,
                lean,
                heading,
                s ^ ((long) cx << 20) ^ cz);
        }
    };

    public final double top, longest, lean;
    private final double[] strands;
    private final double leanX, leanZ;
    private final long salt;

    Virga(double x, double z, double top, double[] strands, double longest, double spread, double lean, double heading,
        long salt) {
        super(x, z, spread + longest * lean + 2, Math.max(1, top - longest - 1), top + 1);
        this.top = top;
        this.longest = longest;
        this.lean = lean;
        this.strands = strands;
        this.leanX = Math.cos(heading) * lean;
        this.leanZ = Math.sin(heading) * lean;
        this.salt = salt;
    }

    @Override
    protected double body(double x, double y, double z) {
        int bx = (int) Math.floor(x), by = (int) Math.floor(y), bz = (int) Math.floor(z);
        double depth = top - by;
        if (depth < 0) return -1;
        for (int i = 0; i < strands.length; i += 3) {
            if (depth > strands[i + 2]) continue;
            if (Math.floor(strands[i] + leanX * depth) != bx || Math.floor(strands[i + 1] + leanZ * depth) != bz)
                continue;
            // Deeper blocks drop out more often, so the streak frays away below.
            if (Hash.hash01(salt, bx, by, bz) >= depth / strands[i + 2]) return 1;
        }
        return -1;
    }
}
