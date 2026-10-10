package chlorine.etjourney.world.end.feature;

import chlorine.etjourney.world.end.noise.Hash;
import chlorine.etjourney.world.end.noise.ValueNoise;

/** BIOSPHERES: a floating glass ball holding a floor of one biome, chosen by its pick. */
public final class Biosphere extends Structure {

    /** What a block of the ball is made of. */
    public enum Part {
        OUTSIDE,
        GLASS,
        AIR,
        TOP,
        FILLER,
        DEEP
    }

    private static final double CEILING = 248;
    /** Clearance over the ground, and the lowest bottom over the void. */
    private static final double OVER_GROUND = 12, VOID_FLOOR = 40;
    /** Blocks of filler under the top block. */
    private static final int FILLER_DEPTH = 3;
    private static final double MAX_RADIUS = 36;

    public static final Structure.Kind<Biosphere> KIND = new Structure.Kind<Biosphere>(
        "BIOSPHERES",
        112,
        MAX_RADIUS + 1) {

        @Override
        protected Biosphere compute(long seed, int cx, int cz, StructureProbe probe) {
            long s = seed ^ 0x3A7D1F9B5C2E8064L;
            if (Hash.hash01(s, cx, cz) > 0.5) return null;
            // Centres keep 0.68 cells apart, wider than two of the largest balls.
            double x = (cx + 0.34 + 0.32 * Hash.hash01(s + 1, cx, cz)) * cell;
            double z = (cz + 0.34 + 0.32 * Hash.hash01(s + 2, cx, cz)) * cell;
            if (!Fade.forms(probe.weight(style, x, z), Hash.hash01(s + 99, cx, cz))) return null;
            double radius = 28 + (MAX_RADIUS - 28) * Hash.hash01(s + 3, cx, cz);
            double ground = probe.ground(x, z);
            double low = (ground > -100 ? ground + OVER_GROUND : VOID_FLOOR) + radius, high = CEILING - radius;
            if (low > high) return null;
            double y = low + (high - low) * 0.6 * Hash.hash01(s + 4, cx, cz);
            return new Biosphere(x, z, y, radius, Hash.hash01(s + 5, cx, cz), s + 31L * cx + cz);
        }
    };

    public final double y, radius;
    /** Which biome fills the ball, as a fraction of the biome list. */
    public final double pick;
    private final long salt;

    Biosphere(double x, double z, double y, double radius, double pick, long salt) {
        super(x, z, radius + 1, y - radius - 1, y + radius + 1);
        this.y = y;
        this.radius = radius;
        this.pick = pick;
        this.salt = salt;
    }

    @Override
    public double[][] feet() {
        return new double[0][];
    }

    @Override
    protected double body(double x, double y, double z) {
        return radius - Math.sqrt(sq(x - centreX) + sq(y - this.y) + sq(z - centreZ));
    }

    /** Height of the top block of the floor in column (x, z): a quarter radius below the centre, gently rolling. */
    public int floorTop(int x, int z) {
        double roll = (ValueNoise.mask(salt, x + 0.5, z + 0.5, 18) - 0.5) * 6;
        return (int) Math.floor(y - radius * 0.25 + roll);
    }

    /** The part of block (x, y, z), judged at its centre. */
    public Part part(int x, int y, int z) {
        double d = Math.sqrt(sq(x + 0.5 - centreX) + sq(y + 0.5 - this.y) + sq(z + 0.5 - centreZ));
        if (d > radius) return Part.OUTSIDE;
        if (d > radius - 1) return Part.GLASS;
        int top = floorTop(x, z);
        if (y > top) return Part.AIR;
        if (y == top) return Part.TOP;
        return y >= top - FILLER_DEPTH ? Part.FILLER : Part.DEEP;
    }

    /** Whether something may grow on the floor of column (x, z), clear of the glass. */
    public boolean onFloor(int x, int z) {
        int top = floorTop(x, z);
        double d = Math.sqrt(sq(x + 0.5 - centreX) + sq(top + 1.5 - y) + sq(z + 0.5 - centreZ));
        return d < radius - 3;
    }

    /** The height a plant of column (x, z) starts at, just above the floor, or -1 where nothing may grow. */
    public int plantY(int x, int z) {
        return onFloor(x, z) ? floorTop(x, z) + 1 : -1;
    }

    private static double sq(double v) {
        return v * v;
    }
}
