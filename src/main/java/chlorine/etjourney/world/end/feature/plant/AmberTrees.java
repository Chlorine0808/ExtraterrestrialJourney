package chlorine.etjourney.world.end.feature.plant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import chlorine.etjourney.world.end.modifier.BlockSink;
import chlorine.etjourney.world.end.reserve.Area;
import chlorine.etjourney.world.end.reserve.Reservations;

/**
 * Decides where the AMBER seed trees stand, per cell and from the seed alone, so any chunk can ask for its neighbours.
 */
public final class AmberTrees {

    public static final int CELL = 32;
    private static final double CHANCE = 0.75;
    /** Distance kept from the cell's edge, so neighbours stay apart. */
    private static final int MARGIN = 4;
    private static final long SALT = 0x6A09E667F3BCC909L;
    /** Spots tried per cell; the one with the flattest footing wins. */
    private static final int CANDIDATES = 6;
    /** Radius of the footing, a little past the legs' feet. */
    private static final int FOOT = 7;
    /** Largest rise across the footing that the legs still reach down; steeper ground gets no tree. */
    private static final int MAX_SPREAD = 6;
    /** Half width of the square the seed rests on. */
    private static final int REST = 3;
    private static final ShapeCache<AmberTree> CACHE = new ShapeCache<>();
    private static final ShapeCache<int[]> FOOTINGS = new ShapeCache<>();

    private AmberTrees() {}

    /** The world as the placement sees it. */
    public interface Terrain {

        double ground(double x, double z);

        /** Whether the topmost surface of the column is AMBER ground. */
        boolean amber(double x, double z);

        /** Shapes are cached per source; a terrain whose ground may differ from generation's needs its own. */
        default long source() {
            return 0;
        }
    }

    /** A tree to grow; {@code y} is the ground plus one. */
    public static final class Anchor {

        public final int x, y, z;
        public final long order;
        public final AmberTree shape;

        Anchor(int x, int y, int z, long order, AmberTree shape) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.order = order;
            this.shape = shape;
        }
    }

    private static Random random(long seed, int cellX, int cellZ) {
        return PlantCells.random(seed, SALT, cellX, cellZ);
    }

    /**
     * Whether generation keeps the tree beside reserved areas. The fade is sampled around the reach as well as at the
     * root, since the legs stand on ground the fade may have lowered.
     */
    public static boolean clearOf(Anchor a, List<Area> reserved) {
        if (reserved.isEmpty()) return true;
        double reach = Math.max(Math.max(-a.shape.minX(), a.shape.maxX()), Math.max(-a.shape.minZ(), a.shape.maxZ()));
        if (Reservations.touches(reserved, a.x, a.z, reach)) return false;
        if (Reservations.suppression(reserved, a.x, a.z) > 0.1) return false;
        for (int k = 0; k < 8; k++) {
            double angle = k * Math.PI / 4;
            double x = a.x + reach * Math.cos(angle), z = a.z + reach * Math.sin(angle);
            if (Reservations.suppression(reserved, x, z) > 0.1) return false;
        }
        return true;
    }

    /** The tree of one 32 x 32 cell, if any. */
    public static List<Anchor> in(long seed, int cellX, int cellZ, Terrain t) {
        Random r = random(seed, cellX, cellZ);
        List<Anchor> out = new ArrayList<>();
        boolean present = r.nextDouble() < CHANCE;
        int[][] spots = new int[CANDIDATES][];
        spots[0] = randomSpot(r, cellX, cellZ);
        long shapeSeed = r.nextLong();
        long order = r.nextLong();
        if (!present) return out;
        for (int i = 1; i < CANDIDATES; i++) spots[i] = randomSpot(r, cellX, cellZ);
        double first = t.ground(spots[0][0] + 0.5, spots[0][1] + 0.5);
        long key = (cellX * PlantCells.K1) ^ (cellZ * PlantCells.K2) ^ (long) Math.floor(first) * SALT ^ t.source();
        int[] foot = FOOTINGS.get(seed, key, () -> footing(spots, t));
        if (foot == null) return out;
        Anchor a = make(seed, foot[0], foot[1], foot[2], shapeSeed, order, t);
        if (a != null) out.add(a);
        return out;
    }

    private static int[] randomSpot(Random r, int cellX, int cellZ) {
        int x = cellX * CELL + MARGIN + r.nextInt(CELL - 2 * MARGIN);
        int z = cellZ * CELL + MARGIN + r.nextInt(CELL - 2 * MARGIN);
        return new int[] { x, z };
    }

    /**
     * The spot with the flattest footing, as {x, z, y}, where y is the root: the lowest ground under the seed plus
     * one, so the seed never floats and every leg reaches down to the ground. Null when no spot is flat enough.
     */
    private static int[] footing(int[][] spots, Terrain t) {
        int[] best = null;
        double bestSpread = Double.MAX_VALUE;
        for (int[] s : spots) {
            if (!t.amber(s[0] + 0.5, s[1] + 0.5)) continue;
            double low = Double.MAX_VALUE, high = -Double.MAX_VALUE, rest = Double.MAX_VALUE;
            for (int dx = -FOOT; dx <= FOOT; dx++) {
                for (int dz = -FOOT; dz <= FOOT; dz++) {
                    if (dx * dx + dz * dz > FOOT * FOOT) continue;
                    double g = Math.floor(t.ground(s[0] + dx + 0.5, s[1] + dz + 0.5));
                    low = Math.min(low, g);
                    high = Math.max(high, g);
                    if (Math.abs(dx) <= REST && Math.abs(dz) <= REST) rest = Math.min(rest, g);
                }
            }
            if (low <= -100 || high - low > MAX_SPREAD || high - low >= bestSpread) continue;
            bestSpread = high - low;
            best = new int[] { s[0], s[1], (int) rest + 1 };
        }
        return best;
    }

    /** Where the cell's first candidate spot lies, {x, z}, before the terrain is asked; null when the cell has none. */
    public static int[] spot(long seed, int cellX, int cellZ) {
        Random r = random(seed, cellX, cellZ);
        if (r.nextDouble() >= CHANCE) return null;
        int x = cellX * CELL + MARGIN + r.nextInt(CELL - 2 * MARGIN);
        int z = cellZ * CELL + MARGIN + r.nextInt(CELL - 2 * MARGIN);
        return new int[] { x, z };
    }

    private static Anchor make(long seed, int x, int z, int y, long shapeSeed, long order, Terrain t) {
        long key = shapeSeed ^ (long) y * PlantCells.K1 ^ t.source() * PlantCells.K2 ^ (long) x * SALT;
        AmberTree shape = CACHE.get(
            seed,
            key,
            () -> AmberTree
                .of(shapeSeed, 250 - y, (dx, dz) -> (int) Math.floor(t.ground(x + dx + 0.5, z + dz + 0.5)) - y));
        return shape == null ? null : new Anchor(x, y, z, order, shape);
    }

    /** Every tree whose cell may reach the chunk, by {@code order}. */
    public static List<Anchor> reaching(long seed, int chunkX, int chunkZ, Terrain t) {
        // A tree spreads about 11 blocks from its foot, less than one cell.
        int cx = Math.floorDiv(chunkX * 16, CELL), cz = Math.floorDiv(chunkZ * 16, CELL);
        List<Anchor> out = new ArrayList<>();
        for (int i = cx - 1; i <= cx + 1; i++) for (int j = cz - 1; j <= cz + 1; j++) out.addAll(in(seed, i, j, t));
        Collections.sort(
            out,
            Comparator.<Anchor>comparingLong(a -> a.order)
                .thenComparingInt(a -> a.x)
                .thenComparingInt(a -> a.z));
        return out;
    }

    /** Writes the trees' cells that fall in the sink, in list order, into air only. */
    public static void draw(List<Anchor> anchors, BlockSink sink) {
        for (Anchor a : anchors) {
            if (!PlantCells.touches(a.x, a.y, a.z, a.shape, sink)) continue;
            a.shape.forEach((dx, dy, dz, block) -> {
                int x = a.x + dx, y = a.y + dy, z = a.z + dz;
                if (HangingRun.inside(sink, x, y, z) && sink.isAir(x, y, z)) sink.place(x, y, z, block);
            });
        }
    }

}
