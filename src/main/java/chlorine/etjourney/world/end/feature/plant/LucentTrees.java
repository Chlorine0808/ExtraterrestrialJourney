package chlorine.etjourney.world.end.feature.plant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import chlorine.etjourney.world.end.modifier.BlockSink;
import chlorine.etjourney.world.end.modifier.EndBlock;

/**
 * Decides where the Lucentwoods trees stand, per cell and from the seed alone, so any chunk can ask for its neighbours.
 */
public final class LucentTrees {

    public static final int CELL = 16;
    /** Upstream's onEveryLayerMax(3): zero to three trees per cell. */
    private static final int MAX_PER_CELL = 3;
    private static final double MIN_SIZE = 12, MAX_SIZE = 20;
    /** Cells around a chunk's own whose trees may reach it; a tree spreads about 15 blocks from its foot. */
    private static final int NEIGHBOURS = 2;
    /** Longest hanging run: a vine of eight. */
    private static final int RUN = 8;
    private static final long SALT = 0x2545F4914F6CDD1DL;
    private static final ShapeCache<LucentTree> CACHE = new ShapeCache<>();

    private LucentTrees() {}

    /** The world as the placement sees it. */
    public interface Terrain {

        double ground(double x, double z);

        /** Whether the topmost surface of the column is Lucentwoods ground. */
        boolean lucent(double x, double z);
    }

    /** A tree to grow; {@code y} is the ground plus one. */
    public static final class Anchor {

        public final int x, y, z;
        public final long order;
        public final LucentTree shape;

        Anchor(int x, int y, int z, long order, LucentTree shape) {
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

    /** The trees of one 16 x 16 cell. */
    public static List<Anchor> in(long seed, int cellX, int cellZ, Terrain t) {
        Random r = random(seed, cellX, cellZ);
        int count = r.nextInt(MAX_PER_CELL + 1);
        List<Anchor> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int x = cellX * CELL + r.nextInt(CELL);
            int z = cellZ * CELL + r.nextInt(CELL);
            double size = MIN_SIZE + r.nextDouble() * (MAX_SIZE - MIN_SIZE);
            long shapeSeed = r.nextLong();
            long order = r.nextLong();
            Anchor a = make(seed, x, z, size, shapeSeed, order, t);
            if (a != null) out.add(a);
        }
        return out;
    }

    private static Anchor make(long seed, int x, int z, double size, long shapeSeed, long order, Terrain t) {
        if (!t.lucent(x + 0.5, z + 0.5)) return null;
        double ground = t.ground(x + 0.5, z + 0.5);
        if (ground <= -100) return null;
        int y = (int) Math.floor(ground) + 1;
        long key = shapeSeed ^ (long) y * PlantCells.K1;
        LucentTree shape = CACHE.get(
            seed,
            key,
            () -> LucentTree
                .of(shapeSeed, size, 250 - y, (dx, dz) -> (int) Math.floor(t.ground(x + dx + 0.5, z + dz + 0.5)) - y));
        return shape == null ? null : new Anchor(x, y, z, order, shape);
    }

    /** Every tree whose cell may reach the chunk, by {@code order}. */
    public static List<Anchor> reaching(long seed, int chunkX, int chunkZ, Terrain t) {
        List<Anchor> out = new ArrayList<>();
        for (int cx = chunkX - NEIGHBOURS; cx <= chunkX + NEIGHBOURS; cx++)
            for (int cz = chunkZ - NEIGHBOURS; cz <= chunkZ + NEIGHBOURS; cz++) out.addAll(in(seed, cx, cz, t));
        Collections.sort(
            out,
            Comparator.<Anchor>comparingLong(a -> a.order)
                .thenComparingInt(a -> a.x)
                .thenComparingInt(a -> a.z));
        return out;
    }

    /** Writes the trees' cells that fall in the sink, in list order, into air only; runs as HangingRun judges. */
    public static void draw(List<Anchor> anchors, BlockSink sink) {
        for (Anchor a : anchors) {
            if (!PlantCells.touches(a.x, a.y, a.z, a.shape, sink)) continue;
            final HangingRun run = new HangingRun(sink, RUN);
            final Set<Long> leaves = new HashSet<>();
            final boolean[] open = { false };
            a.shape.forEach((dx, dy, dz, block) -> {
                int x = a.x + dx, y = a.y + dy, z = a.z + dz;
                if (hanging(block)) {
                    if (!open[0]) {
                        open[0] = true;
                        run.start(y + 1, leaves.contains(pack(dx, dy + 1, dz)));
                    }
                    run.add(x, y, z, block);
                    if (block != EndBlock.LUCENT_VINE) {
                        run.flush();
                        open[0] = false;
                    }
                } else if (HangingRun.inside(sink, x, y, z) && sink.isAir(x, y, z)) {
                    sink.place(x, y, z, block);
                    if (block == EndBlock.LUCENT_LEAVES) leaves.add(pack(dx, dy, dz));
                }
            });
        }
    }

    private static boolean hanging(EndBlock b) {
        return b == EndBlock.LUCENT_VINE || b == EndBlock.LUCENT_VINE_LAST || b == EndBlock.LUCENT_LANTERN;
    }

    private static long pack(int dx, int dy, int dz) {
        return ((long) (dx + 256) << 20) | ((long) (dy + 512) << 10) | (dz + 256);
    }

}
