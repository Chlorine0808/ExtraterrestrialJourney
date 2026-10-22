package chlorine.etjourney.world.end.feature;

import chlorine.etjourney.world.end.noise.ValueNoise;

/** How fully a placed feature forms at its style's weight, so features thin out across a region border. */
public final class Fade {

    /** Weights at which features start to form and fully form. */
    private static final double LOW = 0.1, HIGH = 0.6;

    private Fade() {}

    /** 0 at or below LOW, 1 at or above HIGH, smooth in between. */
    public static double of(double weight) {
        return ValueNoise.smooth(Math.max(0, Math.min(1, (weight - LOW) / (HIGH - LOW))));
    }

    /** Whether a feature with this roll in [0, 1) forms at this weight. */
    public static boolean forms(double weight, double roll) {
        return roll < of(weight);
    }
}
