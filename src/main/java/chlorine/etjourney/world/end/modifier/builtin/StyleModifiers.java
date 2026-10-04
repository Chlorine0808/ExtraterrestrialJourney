package chlorine.etjourney.world.end.modifier.builtin;

import chlorine.etjourney.world.end.feature.Continent;
import chlorine.etjourney.world.end.feature.Relief;
import chlorine.etjourney.world.end.modifier.Layer;
import chlorine.etjourney.world.end.modifier.Modifier;
import chlorine.etjourney.world.end.noise.Fractal;
import chlorine.etjourney.world.end.noise.Hash;
import chlorine.etjourney.world.end.noise.ValueNoise;
import chlorine.etjourney.world.end.noise.Warp;

/** Column modifiers that belong to one style each and apply at that style's weight. */
public final class StyleModifiers {

    /** Highest top of a stacked slab; the tall pass writes everything above Y 127. */
    private static final double MAX_LAYER_TOP = 250;
    private static final int MAX_LAYERS = 5;
    /** Shard size of SHATTERED, and the half-width of the cracks between shards. */
    private static final double SHARD = 56, CRACK = 5;
    /** How far MIRRORED lifts its continent: the widest gap plus a typical reflected slab. */
    private static final double MIRROR_LIFT = 80;

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

    /** Flat-topped terraces: the surface snaps to steps of 8-14 blocks with short steep risers between them. */
    public static Modifier mesas() {
        return CoreModifiers.simple(615, (area, view, s, weight) -> {
            if (s.land <= 0) return;
            long salt = salt(area.seed);
            double step = 8 + 6 * ValueNoise.mask(salt + 40, s.x, s.z, 200);
            double t = s.top / step;
            double f = t - Math.floor(t);
            // Most of each step is flat; its last fifth rises to the next one.
            double riser = f < 0.8 ? 0 : ValueNoise.smooth((f - 0.8) / 0.2);
            double terraced = (Math.floor(t) + riser) * step;
            s.top += (terraced - s.top) * weight * s.interior;
        });
    }

    /**
     * Upside-down continents: the surface stays flat and the mountains, spires and ranges hang below the slab, with
     * stalactite needles of their own.
     */
    public static Modifier inverted() {
        return CoreModifiers.simple(590, (area, view, s, weight) -> {
            long salt = salt(area.seed);
            double hung = s.rise * weight;
            s.rise -= hung;
            s.hills *= 1 - 0.7 * weight;
            double[] w = Continent.warped(area.seed, s.x, s.z);
            double t = Math.max(0, (ValueNoise.mask(salt + 41, w[0], w[1], 36) - 0.55) / 0.45);
            s.hang += hung * 1.3 + weight * s.interior * (1 - s.suppression) * 70 * t * t;
        });
    }

    /** Lifts a MIRRORED continent to leave room for its reflection below. */
    public static Modifier mirroredLift() {
        return CoreModifiers.simple(235, (area, view, s, weight) -> s.level += MIRROR_LIFT * weight * s.interior);
    }

    /** A second slab hangs 30-50 blocks below the continent, its underside mirroring the surface's relief. */
    public static Modifier mirrored() {
        return CoreModifiers.simple(705, (area, view, s, weight) -> {
            double strength = weight * s.interior * (1 - s.suppression);
            if (strength <= 0.05) return;
            long salt = salt(area.seed);
            double gap = 30 + 20 * ValueNoise.mask(salt + 42, s.x, s.z, 150);
            double top = s.bottom - gap;
            double thickness = (s.top - s.bottom) * 0.6 * strength;
            double bottom = top - thickness - Math.max(0, s.top - s.level) * strength;
            if (top - bottom >= 2 && bottom > 1) s.layers.add(new Layer(top, bottom));
        });
    }

    /**
     * The continent breaks into Voronoi shards 40-70 blocks across; each shard rises or sinks by up to 40 blocks and
     * the cracks between them open to the void.
     */
    public static Modifier shattered() {
        return CoreModifiers.simple(205, (area, view, s, weight) -> {
            long salt = salt(area.seed);
            double[] w = Continent.warped(area.seed, s.x, s.z);
            int ox = (int) Math.floor(w[0] / SHARD), oz = (int) Math.floor(w[1] / SHARD);
            double d1 = Double.MAX_VALUE, d2 = Double.MAX_VALUE;
            int bx = 0, bz = 0;
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    int cx = ox + i, cz = oz + j;
                    double px = (cx + 0.15 + 0.7 * Hash.hash01(salt + 43, cx, cz)) * SHARD;
                    double pz = (cz + 0.15 + 0.7 * Hash.hash01(salt + 44, cx, cz)) * SHARD;
                    double d = Math.hypot(w[0] - px, w[1] - pz);
                    if (d < d1) {
                        d2 = d1;
                        d1 = d;
                        bx = cx;
                        bz = cz;
                    } else if (d < d2) d2 = d;
                }
            }
            double offset = (Hash.hash01(salt + 45, bx, bz) - 0.5) * 2 * (10 + 30 * Hash.hash01(salt + 46, bx, bz));
            s.level += offset * weight * s.interior;
            // Half the distance to the next shard's border; cracks are about 10 blocks wide so the grid keeps them.
            double edge = (d2 - d1) / 2;
            if (edge < CRACK) s.land -= (1 - edge / CRACK) * 200 * weight;
        });
    }

    private static long salt(long seed) {
        return seed ^ Relief.SALT;
    }
}
