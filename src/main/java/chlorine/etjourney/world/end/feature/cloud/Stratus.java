package chlorine.etjourney.world.end.feature.cloud;

import java.util.List;

import chlorine.etjourney.world.end.feature.Fade;
import chlorine.etjourney.world.end.modifier.Layer;
import chlorine.etjourney.world.end.noise.ValueNoise;

/** STRATUS: six to twelve sheets one to four blocks thick stacked over the ground, ragged towards the top. */
public final class Stratus {

    /** Highest block a sheet may reach. */
    public static final int TOP = 250;
    private static final long SALT = 0x5C1E9A3F7D2B4086L;
    /** Height the stack starts from where there is no ground. */
    private static final double VOID_BASE = 60;

    private Stratus() {}

    public static void sheets(long seed, double x, double z, double ground, double weight, List<Layer> out) {
        double fade = Fade.of(weight);
        if (fade <= 0) return;
        long s = seed ^ SALT;
        int count = 6 + (int) (6.999 * ValueNoise.mask(s, x, z, 400));
        double y = ground > -100 ? ground : VOID_BASE;
        for (int i = 0; i < count; i++) {
            y += 5 + 5 * ValueNoise.mask(s + 10 + i, x, z, 300);
            double amplitude = 6 + 6 * ValueNoise.mask(s + 30 + i, x, z, 500);
            double centre = y + (ValueNoise.mask(s + 50 + i, x, z, 120) - 0.5) * 2 * amplitude;
            // Upper sheets keep less of their area, so the top of the stack breaks into scraps.
            double coverage = (0.8 - 0.45 * i / (count - 1)) * fade;
            if (ValueNoise.mask(s + 70 + i, x, z, 60) > coverage) continue;
            int bottom = (int) Math.floor(centre);
            int top = bottom + (int) (3.999 * ValueNoise.mask(s + 90 + i, x, z, 90));
            if (top <= TOP) out.add(new Layer(top, bottom));
        }
    }
}
