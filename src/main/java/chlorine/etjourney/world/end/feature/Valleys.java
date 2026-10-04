package chlorine.etjourney.world.end.feature;

import chlorine.etjourney.world.end.noise.ValueNoise;
import chlorine.etjourney.world.end.noise.Warp;

/**
 * Valleys follow the contour where a broad noise crosses its middle value, in a strongly warped frame, so they
 * wander; a second noise switches them on in some regions only. Width and depth drift along a valley.
 */
public final class Valleys {

    private static final double MIN_WIDTH = 0.015, MAX_WIDTH = 0.08;
    private static final double MIN_DEPTH = 8, MAX_DEPTH = 50;

    private Valleys() {}

    public static double depth(long seed, double x, double z) {
        long s = seed ^ Relief.SALT;
        double[] w = Warp.warp(s + 17, x, z, 40, 150);
        double region = (ValueNoise.mask(s + 12, w[0], w[1], 500) - 0.45) / 0.15;
        if (region <= 0) return 0;
        double width = MIN_WIDTH + (MAX_WIDTH - MIN_WIDTH) * ValueNoise.mask(s + 13, w[0], w[1], 300);
        double line = Math.abs(ValueNoise.mask(s + 11, w[0], w[1], 200) - 0.5);
        if (line >= width) return 0;
        double v = 1 - line / width;
        double depth = MIN_DEPTH + (MAX_DEPTH - MIN_DEPTH) * ValueNoise.mask(s + 14, w[0], w[1], 260);
        region = Math.min(1, region);
        return depth * v * v * ValueNoise.smooth(region);
    }
}
