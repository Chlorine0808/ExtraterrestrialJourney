package chlorine.etjourney.world.end.feature;

import chlorine.etjourney.world.end.modifier.EndBlock;
import chlorine.etjourney.world.end.noise.Hash;
import chlorine.etjourney.world.end.noise.ValueNoise;

/** The four zones of the outer End; each zone island takes the zone of its centre. */
public enum Zone {

    SOLAR(EndBlock.SOLAR_TOP, EndBlock.SOLAR_FILL),
    VORTEX(EndBlock.VORTEX_TOP, EndBlock.VORTEX_FILL),
    NEBULA(EndBlock.NEBULA_TOP, EndBlock.NEBULA_FILL),
    STARDUST(EndBlock.STARDUST_TOP, EndBlock.STARDUST_FILL);

    private static final double CELL = 640;
    private static final long SALT = 0x632BE59BD9B4E019L;
    private static final Zone[] ALL = values();

    public final EndBlock top, fill;

    Zone(EndBlock top, EndBlock fill) {
        this.top = top;
        this.fill = fill;
    }

    /** Nearest jittered cell point decides the zone; a light warp keeps borders from looking straight. */
    public static Zone at(long seed, double x, double z) {
        double wx = x + (ValueNoise.mask(seed ^ SALT, x, z, 96) - 0.5) * 120;
        double wz = z + (ValueNoise.mask(seed ^ (SALT + 1), x, z, 96) - 0.5) * 120;
        int cx = (int) Math.floor(wx / CELL), cz = (int) Math.floor(wz / CELL);
        double best = Double.MAX_VALUE;
        Zone zone = SOLAR;
        for (int i = cx - 1; i <= cx + 1; i++) {
            for (int j = cz - 1; j <= cz + 1; j++) {
                double px = (i + Hash.hash01(seed ^ SALT, i, j)) * CELL;
                double pz = (j + Hash.hash01(seed ^ (SALT + 2), i, j)) * CELL;
                double d = (px - wx) * (px - wx) + (pz - wz) * (pz - wz);
                if (d < best) {
                    best = d;
                    zone = ALL[(int) (Hash.hash01(seed ^ (SALT + 3), i, j) * ALL.length)];
                }
            }
        }
        return zone;
    }
}
