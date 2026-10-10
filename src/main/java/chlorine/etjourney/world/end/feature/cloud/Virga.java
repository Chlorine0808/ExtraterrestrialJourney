package chlorine.etjourney.world.end.feature.cloud;

import chlorine.etjourney.world.end.feature.Structure;
import chlorine.etjourney.world.end.feature.StructureProbe;
import chlorine.etjourney.world.end.noise.Hash;

/** VIRGA: six to fifteen one-block streaks hanging from the underside, fraying away further down. */
public final class Virga extends Structure {

    public static final Structure.Kind<Virga> KIND = new Structure.Kind<Virga>("VIRGA", 32, 7) {

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
            double spread = 3 + 3 * Hash.hash01(s + 4, cx, cz);
            // Streaks as {block x, block z, length}.
            double[] strands = new double[count * 3];
            double longest = 0;
            for (int i = 0; i < count; i++) {
                double a = Hash.hash01(s + 10 + i, cx, cz) * Math.PI * 2;
                double d = spread * Math.sqrt(Hash.hash01(s + 30 + i, cx, cz));
                strands[i * 3] = Math.floor(x + Math.cos(a) * d);
                strands[i * 3 + 1] = Math.floor(z + Math.sin(a) * d);
                strands[i * 3 + 2] = 15 + 35 * Hash.hash01(s + 50 + i, cx, cz);
                longest = Math.max(longest, strands[i * 3 + 2]);
            }
            // Starts inside the slab, so the streaks hang from it without a gap.
            return new Virga(x, z, Math.floor(under) + 2, strands, longest, s ^ ((long) cx << 20) ^ cz);
        }
    };

    public final double top, longest;
    private final double[] strands;
    private final long salt;

    Virga(double x, double z, double top, double[] strands, double longest, long salt) {
        super(x, z, 7, Math.max(1, top - longest - 1), top + 1);
        this.top = top;
        this.longest = longest;
        this.strands = strands;
        this.salt = salt;
    }

    @Override
    protected double body(double x, double y, double z) {
        int bx = (int) Math.floor(x), by = (int) Math.floor(y), bz = (int) Math.floor(z);
        double depth = top - by;
        if (depth < 0) return -1;
        for (int i = 0; i < strands.length; i += 3) {
            if (strands[i] != bx || strands[i + 1] != bz || depth > strands[i + 2]) continue;
            // Deeper blocks drop out more often, so the streak frays away below.
            if (Hash.hash01(salt, bx, by, bz) >= depth / strands[i + 2]) return 1;
        }
        return -1;
    }
}
