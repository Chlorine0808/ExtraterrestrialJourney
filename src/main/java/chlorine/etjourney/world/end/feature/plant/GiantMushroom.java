package chlorine.etjourney.world.end.feature.plant;

import java.util.List;
import java.util.Random;
import java.util.Set;

import chlorine.etjourney.world.end.feature.sdf.Sdf;
import chlorine.etjourney.world.end.feature.sdf.SdfFill;
import chlorine.etjourney.world.end.feature.sdf.Spline;
import chlorine.etjourney.world.end.modifier.EndBlock;
import chlorine.etjourney.world.end.noise.ValueNoise;

/** The MYCELIA giant mushrooms, shaped after BetterEnd's mossy glowshroom: flared roots, a bent stem, a wavy plate. */
public enum GiantMushroom implements Grower {

    SMALL(6, 9, 3, 2.0, 2.5, 0.8, 0.75, 1.4, 0.2),
    MEDIUM(12, 18, 6, 4.0, 5.0, 1.2, 0.85, 2.4, 0.5),
    LARGE(22, 30, 8, 5.8, 7.0, 1.7, 1.2, 3.4, 0.5);

    /** Half the cap's thickness: a floor, plus a share of the cap radius. */
    private static final double CAP_HALF = 0.8;
    private static final double CAP_HALF_PER_RADIUS = 0.15;
    /** How far the rim lifts per block from the centre, and how far the noise moves it either way. */
    private static final double LIFT = 0.15;
    private static final double LIFT_NOISE = 0.08;
    private static final double LIFT_NOISE_SCALE = 5;
    /** Rim ripple: lobes, and depth as a share of the cap radius. */
    private static final int RIM_RAYS = 12;
    private static final double RIM_WAVE = 0.07;
    private static final int ROOT_RAYS = 5;

    private final int minHeight;
    private final int maxHeight;
    private final int reach;
    private final double minCap;
    private final double maxCap;
    private final double stemBottom;
    private final double stemTop;
    private final double roots;
    private final double lean;

    GiantMushroom(int minHeight, int maxHeight, int reach, double minCap, double maxCap, double stemBottom,
        double stemTop, double roots, double lean) {
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.reach = reach;
        this.minCap = minCap;
        this.maxCap = maxCap;
        this.stemBottom = stemBottom;
        this.stemTop = stemTop;
        this.roots = roots;
        this.lean = lean;
    }

    @Override
    public int radius() {
        return reach;
    }

    @Override
    public boolean grow(Random random, int x, int y, int z, int headroom, PlantSink out) {
        if (headroom < minHeight) return false;
        int height = Math.min(minHeight + random.nextInt(maxHeight - minHeight + 1), headroom);
        double cap = minCap + random.nextDouble() * (maxCap - minCap);
        double half = CAP_HALF + cap * CAP_HALF_PER_RADIUS;
        // Leave room above the cap's centre for its top and for the rim, which lifts towards the edge.
        double capY = height - 1 - half - cap * (LIFT + LIFT_NOISE) * 0.5;

        int points = Math.max(3, height / 4);
        // The stem stops at the cap's underside; the plate's scooped top would otherwise show its end.
        List<double[]> spline = Spline.make(0, 0, 0, 0, capY - half + 1, 0, points);
        Spline.jitter(spline, random, lean, 0, lean, lean);
        double[] top = spline.get(points - 1);
        Sdf stem = Spline.shape(spline, stemBottom, stemTop);
        Sdf flare = Sdf.sphere(roots)
            .scale(1, 0.7, 1)
            .flatWave(ROOT_RAYS, roots * 0.35, random.nextDouble() * Math.PI * 2);
        stem = stem.smoothUnion(flare, roots * 0.8);

        // As BetterEnd: a cone widening upwards, its top scooped by the same cone at twice the size, and a copy a
        // block lower for thickness, so the lifted rim does not break into steps.
        long seed = random.nextLong();
        double hx = top[0], hz = top[2];
        Sdf plate = Sdf.cappedCone(half, stemTop + 0.5, cap);
        Sdf capShape = plate.subtract(
            plate.scale(2, 2, 2)
                .translate(0, half * 3, 0));
        capShape = capShape.union(capShape.translate(0, -1, 0))
            .flatWave(RIM_RAYS, cap * RIM_WAVE, random.nextDouble() * Math.PI * 2)
            .warp(p -> {
                double d = Math.sqrt(p[0] * p[0] + p[2] * p[2]);
                double n = ValueNoise.mask(seed, p[0] + hx, p[2] + hz, LIFT_NOISE_SCALE) * 2 - 1;
                p[1] += d * (LIFT + n * LIFT_NOISE);
            })
            .translate(hx, capY, hz);

        Sdf shape = stem.smoothUnion(capShape, 1.2);
        int[] bounds = { -reach, 0, -reach, reach, height - 1, reach };
        Set<Long> cells = SdfFill.fill(shape, 0, 0, 0, bounds);
        if (cells.isEmpty()) return false;
        for (long c : cells) {
            int cx = SdfFill.x(c), cy = SdfFill.y(c), cz = SdfFill.z(c);
            out.put(x + cx, y + cy, z + cz, block(c, cells, stem, capShape));
        }
        return true;
    }

    /** Stem outside the cap; under the cap, a cell open below is a gill. */
    private static EndBlock block(long c, Set<Long> cells, Sdf stem, Sdf capShape) {
        if (!isCap(c, stem, capShape)) return EndBlock.MYCELIA_STEM;
        long above = SdfFill.offset(c, 0, 1, 0);
        boolean capAbove = cells.contains(above) && isCap(above, stem, capShape);
        boolean openBelow = !cells.contains(SdfFill.offset(c, 0, -1, 0));
        return capAbove && openBelow ? EndBlock.MYCELIA_GILL : EndBlock.MYCELIA_CAP;
    }

    private static boolean isCap(long c, Sdf stem, Sdf capShape) {
        int x = SdfFill.x(c), y = SdfFill.y(c), z = SdfFill.z(c);
        double d = capShape.distance(x, y, z);
        return d < 0 || d < stem.distance(x, y, z);
    }
}
