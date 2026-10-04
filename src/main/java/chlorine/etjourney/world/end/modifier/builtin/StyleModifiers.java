package chlorine.etjourney.world.end.modifier.builtin;

import chlorine.etjourney.world.end.feature.Continent;
import chlorine.etjourney.world.end.feature.Relief;
import chlorine.etjourney.world.end.modifier.Layer;
import chlorine.etjourney.world.end.modifier.Modifier;
import chlorine.etjourney.world.end.noise.Fractal;
import chlorine.etjourney.world.end.noise.ValueNoise;
import chlorine.etjourney.world.end.noise.Warp;

/** Column modifiers that belong to one style each and apply at that style's weight. */
public final class StyleModifiers {

    /** Highest top of a stacked slab; the tall pass writes everything above Y 127. */
    private static final double MAX_LAYER_TOP = 250;
    private static final int MAX_LAYERS = 5;

    private StyleModifiers() {}

    /** Land sits 28 blocks lower, with half the hills and a thinner slab. */
    public static Modifier lowlands() {
        return CoreModifiers.simple(210, (area, view, s, weight) -> {
            s.level -= 28 * weight;
            s.hillScale *= 1 - 0.5 * weight;
            s.undersideScale *= 1 - 0.4 * weight;
        });
    }

    /** The whole slab bends in broad waves. */
    public static Modifier waves() {
        return CoreModifiers.simple(310, (area, view, s, weight) -> {
            double[] w = Continent.warped(area.seed, s.x, s.z);
            s.level += weight * (Fractal.fbm(salt(area.seed) + 22, w[0], w[1], 70, 3) - 0.5)
                * 80
                * s.interior
                * (1 - s.suppression);
        });
    }

    /** Tighter, steeper waves in a harder-warped frame. */
    public static Modifier wildWaves() {
        return CoreModifiers.simple(320, (area, view, s, weight) -> {
            long salt = salt(area.seed);
            double[] w = Warp.warp(salt + 23, s.x, s.z, 60, 80);
            s.level += weight * (Fractal.fbm(salt + 24, w[0], w[1], 40, 4) - 0.5)
                * 140
                * s.interior
                * (1 - s.suppression);
        });
    }

    /** The region sinks; the style blend across its border becomes the basin's wall. */
    public static Modifier basin() {
        return CoreModifiers.simple(330, (area, view, s, weight) -> s.level -= weight * s.interior * 42);
    }

    /** Needles where a value noise peaks; the 40-block scale keeps each a few density cells wide. */
    public static Modifier spires() {
        return CoreModifiers.simple(410, (area, view, s, weight) -> {
            long salt = salt(area.seed);
            double[] w = Continent.warped(area.seed, s.x, s.z);
            double t = Math.max(0, (ValueNoise.mask(salt + 20, w[0], w[1], 40) - 0.6) / 0.4);
            s.rise += weight * s.interior
                * (1 - s.suppression)
                * 95
                * t
                * t
                * (0.7 + 0.6 * Fractal.fbm(salt + 21, s.x, s.z, 60, 2));
        });
    }

    /** Chains of crests hundreds of blocks long from a ridged fractal. */
    public static Modifier ranges() {
        return CoreModifiers.simple(420, (area, view, s, weight) -> {
            double[] w = Continent.warped(area.seed, s.x, s.z);
            double r = Fractal.ridged(salt(area.seed) + 25, w[0], w[1], 420, 4);
            s.rise += weight * s.interior * (1 - s.suppression) * 165 * Math.pow(r, 2.2);
        });
    }

    /**
     * Stacked slabs above the ground: 2 to 5 depending on the place, each a random 12-34 blocks above the one below,
     * present in patches whose coverage and thickness drift; pillars join the ground to the stack.
     */
    public static Modifier layers() {
        return CoreModifiers.simple(700, (area, view, s, weight) -> {
            double strength = weight * s.interior * (1 - s.suppression);
            if (strength <= 0.05) return;
            long salt = salt(area.seed);
            double[] w = Continent.warped(area.seed, s.x, s.z);
            int count = Math.min(MAX_LAYERS, 2 + (int) (4 * ValueNoise.mask(salt + 32, w[0], w[1], 300)));
            double centre = s.top;
            for (int i = 0; i < count; i++) {
                centre += 12 + 22 * ValueNoise.mask(salt + 70 + i, w[0], w[1], 140);
                double coverage = 0.32 + 0.22 * ValueNoise.mask(salt + 80 + i, w[0], w[1], 220);
                double patch = (Fractal.fbm(salt + 60 + i, w[0], w[1], 90, 3) - coverage) / 0.14;
                if (patch <= 0) continue;
                double half = (2.5 + 2.5 * ValueNoise.mask(salt + 90 + i, w[0], w[1], 100)) * strength
                    * Math.min(1, patch);
                double c = centre + (Fractal.fbm(salt + 100 + i, w[0], w[1], 50, 2) - 0.5) * 10;
                // Near the ceiling a slab thins out instead of ending in a wall.
                half = Math.min(half, MAX_LAYER_TOP - c);
                if (half < 1) continue;
                s.layers.add(new Layer(c + half, c - half * 1.6));
            }
            if (!s.layers.isEmpty()) {
                s.pillar = Math.max(s.pillar, (ValueNoise.mask(salt + 30, w[0], w[1], 32) - 0.82) * 40 * strength);
            }
        });
    }

    private static long salt(long seed) {
        return seed ^ Relief.SALT;
    }
}
