package chlorine.etjourney.world.end.feature;

import chlorine.etjourney.world.end.noise.Hash;
import chlorine.etjourney.world.end.noise.ValueNoise;

/** Splits the plane into warped jittered cells and picks one of n choices per cell. */
final class CellPick {

    private CellPick() {}

    /** Nearest jittered cell point decides; a light warp keeps borders from looking straight. */
    static int at(long seed, long salt, double cell, int choices, double x, double z) {
        double wx = x + (ValueNoise.mask(seed ^ salt, x, z, 96) - 0.5) * 120;
        double wz = z + (ValueNoise.mask(seed ^ (salt + 1), x, z, 96) - 0.5) * 120;
        int cx = (int) Math.floor(wx / cell), cz = (int) Math.floor(wz / cell);
        double best = Double.MAX_VALUE;
        int choice = 0;
        for (int i = cx - 1; i <= cx + 1; i++) {
            for (int j = cz - 1; j <= cz + 1; j++) {
                double px = (i + Hash.hash01(seed ^ salt, i, j)) * cell;
                double pz = (j + Hash.hash01(seed ^ (salt + 2), i, j)) * cell;
                double d = (px - wx) * (px - wx) + (pz - wz) * (pz - wz);
                if (d < best) {
                    best = d;
                    choice = (int) (Hash.hash01(seed ^ (salt + 3), i, j) * choices);
                }
            }
        }
        return choice;
    }
}
