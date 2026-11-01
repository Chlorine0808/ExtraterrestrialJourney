package chlorine.etjourney.world.end.feature.plant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import chlorine.etjourney.world.end.modifier.BlockSink;
import chlorine.etjourney.world.end.modifier.EndBlock;

/** Decides where the glowshrooms stand, per cell and from the seed alone, so any chunk can ask for its neighbours. */
public final class Glowshrooms {

    public static final int NORMAL_CELL = 16, GIANT_CELL = 256, GIANT_MARGIN = 64;
    /** Fill bound of the largest Giant: ceil(22 * 3.5) + 2 = 79, rounded up. */
    public static final int GIANT_REACH = 80;
    /** The y at which the block passes split; a fur run never crosses it. */
    private static final int SPLIT_Y = 128;
    private static final int NORMAL_NEIGHBOURS = 2;
    private static final long SALT = 0x3C6EF372FE94F82BL;
    private static final long GIANT_SALT = 0x5851F42D4C957F2DL;
    private static final long K1 = 0x9E3779B97F4A7C15L, K2 = 0xC2B2AE3D27D4EB4FL;
    private static final Object NONE = new Object();
    private static final Map<Key, Object> NORMALS = lru(512);
    private static final Map<Key, Object> GIANTS = lru(32);
    private static long cachedSeed;
    private static boolean seeded;

    private Glowshrooms() {}

    /** The world as the placement sees it. */
    public interface Terrain {

        double ground(double x, double z);

        /** Whether the topmost surface of the column is mycelia. */
        boolean mycelia(double x, double z);
    }

    /** A glowshroom to grow; {@code y} is the ground plus one. */
    public static final class Anchor {

        public final int x, y, z;
        public final boolean giant;
        public final long order;
        public final Glowshroom shape;

        Anchor(int x, int y, int z, boolean giant, long order, Glowshroom shape) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.giant = giant;
            this.order = order;
            this.shape = shape;
        }
    }

    private static final class Key {

        final long seed;
        final int x, y, z;
        final boolean giant;

        Key(long seed, int x, int y, int z, boolean giant) {
            this.seed = seed;
            this.x = x;
            this.y = y;
            this.z = z;
            this.giant = giant;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Key)) return false;
            Key k = (Key) o;
            return seed == k.seed && x == k.x && y == k.y && z == k.z && giant == k.giant;
        }

        @Override
        public int hashCode() {
            return ((Long.hashCode(seed) * 31 + x) * 31 + y) * 31 + z + (giant ? 1 : 0);
        }
    }

    private static Map<Key, Object> lru(final int max) {
        return new LinkedHashMap<Key, Object>(16, 0.75f, true) {

            @Override
            protected boolean removeEldestEntry(Map.Entry<Key, Object> e) {
                return size() > max;
            }
        };
    }

    private static Random random(long seed, long salt, int cellX, int cellZ) {
        Random r = new Random(seed ^ salt ^ cellX * K1 ^ cellZ * K2);
        // Rehash: Random seeds that are close give nearly equal first draws.
        return new Random(r.nextLong() ^ r.nextLong());
    }

    /** The glowshrooms of one 16 x 16 cell. */
    public static List<Anchor> normalIn(long seed, int cellX, int cellZ, Terrain t) {
        Random r = random(seed, SALT, cellX, cellZ);
        int count = r.nextInt(3);
        List<Anchor> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int x = cellX * NORMAL_CELL + r.nextInt(NORMAL_CELL);
            int z = cellZ * NORMAL_CELL + r.nextInt(NORMAL_CELL);
            double scale = 0.75 + r.nextDouble() * 0.35;
            long shapeSeed = r.nextLong();
            long order = r.nextLong();
            Anchor a = make(seed, x, z, scale, shapeSeed, order, false, t, NORMALS);
            if (a != null) out.add(a);
        }
        return out;
    }

    /** The Giant of one 256 x 256 cell, or null. */
    public static Anchor giantIn(long seed, int cellX, int cellZ, Terrain t) {
        Random r = random(seed, SALT ^ GIANT_SALT, cellX, cellZ);
        int span = GIANT_CELL - 2 * GIANT_MARGIN;
        int x = cellX * GIANT_CELL + GIANT_MARGIN + r.nextInt(span);
        int z = cellZ * GIANT_CELL + GIANT_MARGIN + r.nextInt(span);
        double scale = 2 + r.nextDouble() * 1.5;
        long shapeSeed = r.nextLong();
        long order = r.nextLong();
        return make(seed, x, z, scale, shapeSeed, order, true, t, GIANTS);
    }

    private static Anchor make(long seed, int x, int z, double scale, long shapeSeed, long order, boolean giant,
        Terrain t, Map<Key, Object> cache) {
        if (!t.mycelia(x + 0.5, z + 0.5)) return null;
        double ground = t.ground(x + 0.5, z + 0.5);
        if (ground <= -100) return null;
        int y = (int) Math.floor(ground) + 1;
        Key key = new Key(seed, x, y, z, giant);
        Object shape;
        synchronized (Glowshrooms.class) {
            if (!seeded || seed != cachedSeed) {
                NORMALS.clear();
                GIANTS.clear();
                cachedSeed = seed;
                seeded = true;
            }
            shape = cache.get(key);
        }
        if (shape == null) {
            Glowshroom built = Glowshroom.of(shapeSeed, scale, giant, 250 - y);
            shape = built == null ? NONE : built;
            synchronized (Glowshrooms.class) {
                cache.put(key, shape);
            }
        }
        return shape == NONE ? null : new Anchor(x, y, z, giant, order, (Glowshroom) shape);
    }

    /** Every anchor whose cells may reach the chunk: Giants first, then normals, each by {@code order}. */
    public static List<Anchor> reaching(long seed, int chunkX, int chunkZ, Terrain t) {
        List<Anchor> giants = new ArrayList<>();
        int x0 = chunkX * 16 - GIANT_REACH, x1 = chunkX * 16 + 15 + GIANT_REACH;
        int z0 = chunkZ * 16 - GIANT_REACH, z1 = chunkZ * 16 + 15 + GIANT_REACH;
        for (int cx = Math.floorDiv(x0, GIANT_CELL); cx <= Math.floorDiv(x1, GIANT_CELL); cx++)
            for (int cz = Math.floorDiv(z0, GIANT_CELL); cz <= Math.floorDiv(z1, GIANT_CELL); cz++) {
                Anchor g = giantIn(seed, cx, cz, t);
                if (g != null) giants.add(g);
            }
        List<Anchor> normals = new ArrayList<>();
        for (int cx = chunkX - NORMAL_NEIGHBOURS; cx <= chunkX + NORMAL_NEIGHBOURS; cx++)
            for (int cz = chunkZ - NORMAL_NEIGHBOURS; cz <= chunkZ + NORMAL_NEIGHBOURS; cz++)
                normals.addAll(normalIn(seed, cx, cz, t));
        Comparator<Anchor> byOrder = Comparator.<Anchor>comparingLong(a -> a.order)
            .thenComparingInt(a -> a.x)
            .thenComparingInt(a -> a.z);
        Collections.sort(giants, byOrder);
        Collections.sort(normals, byOrder);
        giants.addAll(normals);
        return giants;
    }

    /**
     * Writes the anchors' cells that fall in the sink, in list order, into air only. A fur run belongs to the half
     * (below or above {@code SPLIT_Y}) that holds its gill, and its cells in the other half are cut; it is written
     * only when its gill was placed and every cell of it in the sink is air. So both halves, and any chunk, reach
     * the same verdict.
     */
    public static void draw(List<Anchor> anchors, BlockSink sink) {
        for (Anchor a : anchors) {
            final Run run = new Run(sink);
            final Set<Long> gills = new HashSet<>();
            final boolean[] open = { false };
            a.shape.forEach((dx, dy, dz, block) -> {
                int x = a.x + dx, y = a.y + dy, z = a.z + dz;
                if (block == EndBlock.MYCELIA_VINE || block == EndBlock.MYCELIA_VINE_LAST) {
                    if (!open[0]) {
                        open[0] = true;
                        run.start(y + 1, gills.contains(pack(dx, dy + 1, dz)));
                    }
                    run.add(x, y, z, block);
                    if (block == EndBlock.MYCELIA_VINE_LAST) {
                        run.flush();
                        open[0] = false;
                    }
                } else {
                    if (open[0]) {
                        run.flush();
                        open[0] = false;
                    }
                    if (inside(sink, x, y, z) && sink.isAir(x, y, z)) {
                        sink.place(x, y, z, block);
                        if (block == EndBlock.MYCELIA_GILL) gills.add(pack(dx, dy, dz));
                    }
                }
            });
            run.flush();
        }
    }

    private static long pack(int dx, int dy, int dz) {
        return ((long) (dx + 256) << 20) | ((long) (dy + 512) << 10) | (dz + 256);
    }

    private static boolean inside(BlockSink s, int x, int y, int z) {
        return x >= s.originX() && x < s.originX() + s.width()
            && z >= s.originZ()
            && z < s.originZ() + s.width()
            && y >= s.minY()
            && y < s.maxY();
    }

    /** The fur cells of one run, collected so the run can be judged whole. */
    private static final class Run {

        private final BlockSink sink;
        private final int[] xs = new int[4], ys = new int[4], zs = new int[4];
        private final EndBlock[] kinds = new EndBlock[4];
        private int n, half;
        private boolean gillPlaced;

        Run(BlockSink sink) {
            this.sink = sink;
        }

        void start(int gillY, boolean placed) {
            n = 0;
            half = gillY / SPLIT_Y;
            gillPlaced = placed;
        }

        void add(int x, int y, int z, EndBlock b) {
            if (y / SPLIT_Y != half || n == kinds.length) return;
            xs[n] = x;
            ys[n] = y;
            zs[n] = z;
            kinds[n++] = b;
        }

        void flush() {
            boolean clear = gillPlaced;
            for (int i = 0; i < n && clear; i++)
                if (inside(sink, xs[i], ys[i], zs[i]) && !sink.isAir(xs[i], ys[i], zs[i])) clear = false;
            if (clear) for (int i = 0; i < n; i++)
                if (inside(sink, xs[i], ys[i], zs[i])) sink.place(xs[i], ys[i], zs[i], kinds[i]);
            n = 0;
        }
    }
}
