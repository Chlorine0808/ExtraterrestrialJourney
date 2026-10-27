package chlorine.etjourney.world.end;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;

import chlorine.etjourney.core.util.ModLog;
import chlorine.etjourney.world.end.feature.EndBiome;
import chlorine.etjourney.world.end.feature.Flora;
import chlorine.etjourney.world.end.noise.Distance;
import cpw.mods.fml.common.IWorldGenerator;
import cpw.mods.fml.common.registry.GameRegistry;

/** Sets the biome plants on the painted surfaces at populate time, in the window offset by 8 as vanilla does. */
public final class EndFlora implements IWorldGenerator {

    private static final int END = 1;
    /** Farthest a block of the window lies from its centre, rounded up. */
    private static final int WINDOW_REACH = 12;
    private static final EndBiome[] BIOMES = EndBiome.values();
    private static final AtomicLong NANOS = new AtomicLong(), WINDOWS = new AtomicLong();

    private boolean warned;

    public static void register() {
        GameRegistry.registerWorldGenerator(new EndFlora(), 5);
    }

    /** Average microseconds per planted window, and the number of windows, for the probe. */
    public static long[] stats() {
        long windows = WINDOWS.get();
        return new long[] { windows == 0 ? 0 : NANOS.get() / windows / 1000, windows };
    }

    /** Which of the four chunks of chunk (chunkX, chunkZ)'s window holds (x, z): x step 2, z step 1; -1 outside. */
    static int chunkIndex(int chunkX, int chunkZ, int x, int z) {
        int i = (x >> 4) - chunkX, j = (z >> 4) - chunkZ;
        return i < 0 || i > 1 || j < 0 || j > 1 ? -1 : i * 2 + j;
    }

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider generator,
        IChunkProvider provider) {
        if (world.isRemote || world.provider.dimensionId != END) return;
        int ox = chunkX * 16 + 8, oz = chunkZ * 16 + 8;
        if (Distance.planar(ox + 8, oz + 8) < EndTerrain.TERRAIN_START - WINDOW_REACH) return;
        long start = System.nanoTime();
        try {
            plant(random, chunkX, chunkZ, world);
        } catch (RuntimeException e) {
            // A last guard: plants must never take End generation down with them.
            if (!warned)
                ModLog.LOG.warn("End plants failed at chunk {},{}; later failures stay quiet", chunkX, chunkZ, e);
            warned = true;
        }
        NANOS.addAndGet(System.nanoTime() - start);
        WINDOWS.incrementAndGet();
    }

    private static void plant(Random random, int chunkX, int chunkZ, World world) {
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
        if (!any) return;
        Flora.Ground ground = (x, z, ys, biomes) -> {
            int i = chunkIndex(chunkX, chunkZ, x, z);
            return i < 0 || chunks[i] == null ? 0 : Flora.scan(new ChunkColumn(chunks[i], x & 15, z & 15), ys, biomes);
        };
        Flora.plant(random, chunkX * 16 + 8, chunkZ * 16 + 8, ground, (flora, x, y, z) -> {
            int i = chunkIndex(chunkX, chunkZ, x, z);
            if (i < 0 || chunks[i] == null || y < 1 || y > 255 || !EndPalette.isBound(flora.block)) return;
            if (chunks[i].getBlock(x & 15, y, z & 15)
                .getMaterial() != Material.air) return;
            // Flag 2: tell clients, but wake no neighbours.
            world.setBlock(x, y, z, EndPalette.block(flora.block), EndPalette.meta(flora.block), 2);
        });
    }

    /** One column of a loaded chunk, telling biome surface blocks by the blocks content bound to them. */
    private static final class ChunkColumn implements Flora.Column {

        private final Chunk chunk;
        private final int x, z;

        ChunkColumn(Chunk chunk, int x, int z) {
            this.chunk = chunk;
            this.x = x;
            this.z = z;
        }

        @Override
        public int top() {
            return chunk.getTopFilledSegment() + 15;
        }

        @Override
        public EndBiome surfaceAt(int y) {
            Block block = chunk.getBlock(x, y, z);
            if (block.getMaterial() == Material.air) return null;
            for (EndBiome biome : BIOMES) {
                if (EndPalette.isBound(biome.top) && EndPalette.block(biome.top) == block
                    && chunk.getBlockMetadata(x, y, z) == EndPalette.meta(biome.top)) return biome;
            }
            return null;
        }

        @Override
        public boolean isAir(int y) {
            return y > 255 || chunk.getBlock(x, y, z)
                .getMaterial() == Material.air;
        }
    }
}
