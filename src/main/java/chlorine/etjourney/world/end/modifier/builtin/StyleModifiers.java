package chlorine.etjourney.world.end.modifier.builtin;

import chlorine.etjourney.world.end.feature.Canyons;
import chlorine.etjourney.world.end.feature.Continent;
import chlorine.etjourney.world.end.feature.Relief;
import chlorine.etjourney.world.end.modifier.ColumnState;
import chlorine.etjourney.world.end.modifier.Layer;
import chlorine.etjourney.world.end.modifier.Modifier;
import chlorine.etjourney.world.end.noise.Fractal;
import chlorine.etjourney.world.end.noise.Hash;
import chlorine.etjourney.world.end.noise.ValueNoise;
import chlorine.etjourney.world.end.noise.Warp;

/** Modifiers that belong to one style each and apply at that style's weight. */
public final class StyleModifiers {

    /** Highest top of a stacked slab; the tall pass writes everything above Y 127. */
    private static final double MAX_LAYER_TOP = 250;
    private static final int MAX_LAYERS = 5;
    /** Shard size of SHATTERED, and the half-width of the cracks between shards. */
    private static final double SHARD = 56, CRACK = 7;
    /** How far MIRRORED lifts its continent: the widest gap plus a typical reflected slab. */
    private static final double MIRROR_LIFT = 80;
    /** How far INVERTED lifts its continent for the relief hanging below. */
    private static final double INVERT_LIFT = 70;

    /**
     * LAYERED tier archetypes as {low, high} ranges of hole scale, coverage threshold, half thickness and swell:
     * broad thin sheets, chunky slabs, and scattered fragments.
     */
    private static final double[][][] TIER_ARCHETYPES = { { { 120, 180 }, { 0.36, 0.48 }, { 1.5, 3 }, { 10, 30 } },
        { { 60, 110 }, { 0.48, 0.60 }, { 6, 10 }, { 4, 14 } }, { { 30, 50 }, { 0.58, 0.68 }, { 3, 5 }, { 8, 24 } } };

    private StyleModifiers() {}

    private static double lerp(double[] range, double t) {
        return range[0] + (range[1] - range[0]) * t;
    }

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
     * Stacked slabs above the continent. Each tier has its own warp and character (hole size, coverage, thickness,
     * undulation, keel), drifting over about 500 blocks, so the tiers of one column do not repeat each other.
     */
    public static Modifier layers() {
        return CoreModifiers.simple(700, (area, view, s, weight) -> {
            double strength = weight * s.interior * (1 - s.suppression);
            if (strength <= 0.05) return;
            long salt = salt(area.seed);
            double[] w = Continent.warped(area.seed, s.x, s.z);
            int count = Math.min(MAX_LAYERS, 2 + (int) (4 * ValueNoise.mask(salt + 32, w[0], w[1], 300)));
            double centre = s.top;
            // Neighbouring tiers take turns through the archetypes, from a phase that drifts between regions.
            double phase = tierPhase(salt, s.x, s.z);
            for (int i = 0; i < count; i++) {
                Tier tier = tier(salt, i, phase, s.x, s.z);
                centre += tier.step;
                if (tier.patch <= 0) continue;
                double half = tier.thick * strength * Math.min(1, tier.patch);
                double c = centre + tier.offset;
                // Near the ceiling a slab thins out instead of ending in a wall.
                half = Math.min(half, MAX_LAYER_TOP - c);
                if (half < 1) continue;
                s.layers.add(new Layer(c + half, c - half * tier.keel));
            }
            if (!s.layers.isEmpty()) {
                s.pillar = Math.max(s.pillar, (ValueNoise.mask(salt + 30, w[0], w[1], 32) - 0.82) * 40 * strength);
            }
        });
    }

    /** One LAYERED tier at a column: rise from the tier below, patch (positive where present), thickness, offset. */
    static final class Tier {

        double step, patch, thick, offset, keel;
    }

    /** Which archetype tier 0 takes, as a number in [0, archetypes); tier i takes the one i places on. */
    static double tierPhase(long salt, double x, double z) {
        return TIER_ARCHETYPES.length * ValueNoise.mask(salt + 33, x, z, 600);
    }

    static Tier tier(long salt, int i, double phase, double x, double z) {
        long t = salt + 1000L * (i + 1);
        double[] wi = Warp.warp(t, x, z, 48, 170);
        Tier tier = new Tier();
        tier.keel = 1.1 + 1.2 * ValueNoise.mask(t + 5, x, z, 460);
        tier.step = 14 + 24 * ValueNoise.mask(t + 6, wi[0], wi[1], 200);
        archetype(tier, t, ((int) phase + i) % TIER_ARCHETYPES.length, wi, x, z, 1);
        return tier;
    }

    /** Adds an archetype's patch, thickness and offset to the tier, at the given share. */
    private static void archetype(Tier tier, long t, int k, double[] wi, double x, double z, double share) {
        double[][] type = TIER_ARCHETYPES[k];
        long u = t + 100L * (k + 1);
        // One hole scale per tier and archetype: a scale that drifts would squeeze the lattice further from the origin.
        double scale = lerp(type[0], Hash.hash01(u, 0, 0));
        double coverage = lerp(type[1], ValueNoise.mask(u + 2, x, z, 480));
        double swell = lerp(type[3], ValueNoise.mask(u + 4, x, z, 540));
        tier.patch += share * (Fractal.fbm(u + 7, wi[0], wi[1], scale, 3) - coverage) / 0.14;
        tier.thick += share * lerp(type[2], ValueNoise.mask(u + 3, x, z, 500))
            * (0.7 + 0.6 * ValueNoise.mask(u + 8, wi[0], wi[1], scale * 0.8));
        tier.offset += share * (Fractal.fbm(t + 9, wi[0], wi[1], 110, 3) - 0.5) * swell;
    }

    /**
     * Slot canyons cut into the land, to an absolute floor below the column top so both halves of a tall column cut
     * the same blocks. The style fades the depth in at its borders.
     */
    public static Modifier slotCanyons() {
        return FeatureModifiers.blockModifier(812, (area, view, sink) -> {
            for (int x = sink.originX(); x < sink.originX() + 16; x++) {
                for (int z = sink.originZ(); z < sink.originZ() + 16; z++) {
                    double depth = Canyons.depth(area.seed, x, z);
                    if (depth <= 0) continue;
                    double fade = Math.min(1, (view.weight("SLOT_CANYONS", x, z) - 0.3) / 0.4);
                    if (fade <= 0) continue;
                    ColumnState c = view.column(x, z);
                    if (c.land <= 0) continue;
                    int floor = (int) Math.ceil(Math.max(c.top - depth * fade, c.bottom + 6));
                    // Above the column top for the density noise and anything the surface modifiers raised.
                    int ceiling = (int) c.top + 16;
                    for (int y = Math.max(floor, sink.minY()); y <= Math.min(ceiling, sink.maxY() - 1); y++) {
                        sink.clear(x, y, z);
                    }
                }
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

    /** Lifts an INVERTED continent to leave room for the relief hanging below it. */
    public static Modifier invertedLift() {
        return CoreModifiers.simple(236, (area, view, s, weight) -> s.level += INVERT_LIFT * weight * s.interior);
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
            // Half the distance to the next shard's border. The land drops fully across the crack but its last block,
            // so a crack is wider than the 8-block density grid and every crossing keeps a grid column in it.
            double edge = (d2 - d1) / 2;
            if (edge < CRACK) s.land -= Math.min(1, CRACK - edge) * 200 * weight;
        });
    }

    private static long salt(long seed) {
        return seed ^ Relief.SALT;
    }
}
