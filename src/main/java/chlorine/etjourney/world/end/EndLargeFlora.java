package chlorine.etjourney.world.end;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;

import chlorine.etjourney.core.util.ModLog;
import chlorine.etjourney.world.end.feature.EndBiome;
import chlorine.etjourney.world.end.feature.Flora;
import chlorine.etjourney.world.end.feature.plant.LargeFlora;
import chlorine.etjourney.world.end.noise.Distance;
import cpw.mods.fml.common.IWorldGenerator;
import cpw.mods.fml.common.registry.GameRegistry;

/** Grows the large plants at populate time, in the same window as EndFlora and before its small plants. */
public final class EndLargeFlora implements IWorldGenerator {

    private static final int END = 1;
    /** Farthest a block of the window lies from its centre, rounded up; large plants reach 8 past the window. */
    private static final int WINDOW_REACH = 20;
    private static final int MAX_HEADROOM = 64;
    private static final AtomicLong NANOS = new AtomicLong(), WINDOWS = new AtomicLong(), PLANTS = new AtomicLong();

    private boolean warned;

    public static void register() {
        // FML runs the lighter weights first, so this goes ahead of EndFlora (5).
        GameRegistry.registerWorldGenerator(new EndLargeFlora(), 4);
    }

    /** Average microseconds per window, the number of windows, and the number of large plants grown. */
    public static long[] stats() {
        long windows = WINDOWS.get();
        return new long[] { windows == 0 ? 0 : NANOS.get() / windows / 1000, windows, PLANTS.get() };
    }

    /** Whether a plant block at (x, y, z) lies in the four chunks of chunk (chunkX, chunkZ)'s window and in range. */
    static boolean writable(int chunkX, int chunkZ, int x, int y, int z) {
        return y >= 1 && y <= 255 && EndFlora.chunkIndex(chunkX, chunkZ, x, z) >= 0;
    }

    /** Mixed into the populate seed so the large plants do not draw the same numbers as EndFlora. */
    private static final long SALT = 0x5DEECE66DL * 0x2545F4914F6CDD1DL;

    /**
     * The Random the large plants draw from. FML seeds every generator's Random the same way, so without the salt the
     * first tree would land on EndFlora's first bloom patch.
     */
    static Random ownRandom(Random populate) {
        return new Random(populate.nextLong() ^ SALT);
    }

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider generator,
        IChunkProvider provider) {
        if (world.isRemote || world.provider.dimensionId != END) return;
        int ox = chunkX * 16 + 8, oz = chunkZ * 16 + 8;
        if (Distance.planar(ox + 8, oz + 8) < EndTerrain.TERRAIN_START - WINDOW_REACH) return;
        long start = System.nanoTime();
        try {
            PLANTS.addAndGet(plant(ownRandom(random), chunkX, chunkZ, world));
        } catch (RuntimeException e) {
            // A last guard: plants must never take End generation down with them.
            if (!warned)
                ModLog.LOG.warn("End large plants failed at chunk {},{}; later failures stay quiet", chunkX, chunkZ, e);
            warned = true;
        }
        NANOS.addAndGet(System.nanoTime() - start);
        WINDOWS.incrementAndGet();
    }

    private static int plant(Random random, int chunkX, int chunkZ, World world) {
        // Populate runs once these four chunks exist; check anyway, so a missing one is skipped, never generated.
        Chunk[] chunks = new Chunk[4];
        boolean any = false;
        for (int i = 0; i < 4; i++) {
            int cx = chunkX + (i >> 1), cz = chunkZ + (i & 1);
            if (!world.getChunkProvider()
                .chunkExists(cx, cz)) continue;
            chunks[i] = world.getChunkFromChunkCoords(cx, cz);
            any = true;
        }
        if (!any) return 0;
        LargeFlora.Ground ground = new LargeFlora.Ground() {

            @Override
            public int surfaces(int x, int z, int[] ys, EndBiome[] biomes) {
                int i = EndFlora.chunkIndex(chunkX, chunkZ, x, z);
                return i < 0 || chunks[i] == null ? 0
                    : Flora.scan(new EndFlora.ChunkColumn(chunks[i], x & 15, z & 15), ys, biomes);
            }

            @Override
            public int headroom(int x, int y, int z) {
                int i = EndFlora.chunkIndex(chunkX, chunkZ, x, z);
                if (i < 0 || chunks[i] == null) return 0;
                EndFlora.ChunkColumn column = new EndFlora.ChunkColumn(chunks[i], x & 15, z & 15);
                int n = 0;
                // Stops at the build limit, at MAX_HEADROOM, and at the first block that is not air.
                while (n < MAX_HEADROOM && y + n <= 255 && column.isAir(y + n)) n++;
                return n;
            }
        };
        return LargeFlora.plant(random, chunkX * 16 + 8, chunkZ * 16 + 8, ground, (x, y, z, block) -> {
            if (!writable(chunkX, chunkZ, x, y, z) || !EndPalette.isBound(block)) return;
            int i = EndFlora.chunkIndex(chunkX, chunkZ, x, z);
            if (chunks[i] == null) return;
            Block at = chunks[i].getBlock(x & 15, y, z & 15);
            if (!at.isAir(world, x, y, z) && !at.isReplaceable(world, x, y, z)) return;
            // Flag 2: tell clients, but wake no neighbours.
            world.setBlock(x, y, z, EndPalette.block(block), EndPalette.meta(block), 2);
        });
    }
}
