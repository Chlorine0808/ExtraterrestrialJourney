package chlorine.etjourney.world.end.feature.plant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private static final long K1 = 0x9E3779B97F4A7C15L, K2 = 0xC2B2AE3D27D4EB4FL;
    private static final Object NONE = new Object();
    private static final Map<Long, Object> CACHE = new LinkedHashMap<Long, Object>(16, 0.75f, true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Object> e) {
            return size() > 512;
        }
    };
    private static long cachedSeed;
    private static boolean seeded;

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
        Random r = new Random(seed ^ SALT ^ cellX * K1 ^ cellZ * K2);
        // Rehash: Random seeds that are close give nearly equal first draws.
        return new Random(r.nextLong() ^ r.nextLong());
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
        int x = cellX * CELL + MARGIN + r.nextInt(CELL - 2 * MARGIN);
        int z = cellZ * CELL + MARGIN + r.nextInt(CELL - 2 * MARGIN);
        long shapeSeed = r.nextLong();
        long order = r.nextLong();
        if (!present) return out;
        Anchor a = make(seed, x, z, shapeSeed, order, t);
        if (a != null) out.add(a);
        return out;
    }

    /** Where the cell's tree would stand, {x, z}, before the terrain is asked; null when the cell has none. */
    public static int[] spot(long seed, int cellX, int cellZ) {
        Random r = random(seed, cellX, cellZ);
        if (r.nextDouble() >= CHANCE) return null;
        int x = cellX * CELL + MARGIN + r.nextInt(CELL - 2 * MARGIN);
        int z = cellZ * CELL + MARGIN + r.nextInt(CELL - 2 * MARGIN);
        return new int[] { x, z };
    }

    private static Anchor make(long seed, int x, int z, long shapeSeed, long order, Terrain t) {
        if (!t.amber(x + 0.5, z + 0.5)) return null;
        double ground = t.ground(x + 0.5, z + 0.5);
        if (ground <= -100) return null;
        int y = (int) Math.floor(ground) + 1;
        long key = shapeSeed ^ (long) y * K1 ^ t.source() * K2;
        Object shape;
        synchronized (AmberTrees.class) {
            if (!seeded || seed != cachedSeed) {
                CACHE.clear();
                cachedSeed = seed;
                seeded = true;
            }
            shape = CACHE.get(key);
        }
        if (shape == null) {
            AmberTree built = AmberTree
                .of(shapeSeed, 250 - y, (dx, dz) -> (int) Math.floor(t.ground(x + dx + 0.5, z + dz + 0.5)) - y);
            shape = built == null ? NONE : built;
            synchronized (AmberTrees.class) {
                CACHE.put(key, shape);
            }
        }
        return shape == NONE ? null : new Anchor(x, y, z, order, (AmberTree) shape);
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
            if (!touches(a, sink)) continue;
            a.shape.forEach((dx, dy, dz, block) -> {
                int x = a.x + dx, y = a.y + dy, z = a.z + dz;
                if (HangingRun.inside(sink, x, y, z) && sink.isAir(x, y, z)) sink.place(x, y, z, block);
            });
        }
    }

    /** Whether the anchor's bounding box meets the sink's volume. */
    private static boolean touches(Anchor a, BlockSink s) {
        return a.x + a.shape.maxX() >= s.originX() && a.x + a.shape.minX() < s.originX() + s.width()
            && a.z + a.shape.maxZ() >= s.originZ()
            && a.z + a.shape.minZ() < s.originZ() + s.width()
            && a.y + a.shape.maxY() >= s.minY()
            && a.y + a.shape.minY() < s.maxY();
    }
}
