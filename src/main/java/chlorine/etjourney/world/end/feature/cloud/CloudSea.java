package chlorine.etjourney.world.end.feature.cloud;

import java.util.List;

import chlorine.etjourney.world.end.feature.Fade;
import chlorine.etjourney.world.end.modifier.Layer;
import chlorine.etjourney.world.end.noise.ValueNoise;

/** CLOUD_SEA: one level sheet around Y 100, with holes onto the void and peaks standing through it. */
public final class CloudSea {

    /** Mean height of the sheet's top. */
    public static final double LEVEL = 100;
    private static final long SALT = 0x6A3F1D8C5E2B9074L;

    private CloudSea() {}

    /** Height of the sheet's top at (x, z): level across a region, swaying 4 blocks over 400. */
    public static double level(long seed, double x, double z) {
        return LEVEL + (ValueNoise.mask(seed ^ SALT, x, z, 400) - 0.5) * 8;
    }

    public static void sheets(long seed, double x, double z, double ground, double weight, List<Layer> out) {
        double fade = Fade.of(weight);
        if (fade <= 0) return;
        long s = seed ^ SALT;
        double sea = level(seed, x, z);
        // Ground that reaches the sheet is a peak standing through it.
        if (ground >= sea - 1) return;
        // Holes cover 30% at full weight and widen until nothing is left at the border.
        if (ValueNoise.mask(s + 1, x, z, 80) < 1 - 0.7 * fade) return;
        int top = (int) Math.floor(sea);
        int thick = 3 + (int) (3.999 * ValueNoise.mask(s + 2, x, z, 50));
        int lump = (int) (2.999 * ValueNoise.mask(s + 3, x, z, 9));
        out.add(new Layer(top + lump, top - thick + 1));
    }
}
