package chlorine.etjourney.world.end;

import java.util.concurrent.atomic.AtomicLong;

import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.ChunkProviderEnd;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.ChunkProviderEvent;
import net.minecraftforge.event.world.ChunkEvent;

import chlorine.etjourney.world.end.region.RegionPicker;
import chlorine.etjourney.world.end.region.Styles;
import chlorine.etjourney.world.end.reserve.Reservations;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Hooks the terrain engine into the vanilla End generator (which HEE extends): density in InitNoiseField, block
 * work in ReplaceBiomeBlocks, and terrain above Y 127 on ChunkEvent.Load before the chunk is populated.
 */
public final class EndTerrain {

    /** Inside this radius the vanilla generator keeps the central island. */
    public static final double TERRAIN_START = 400;
    private static final int END = 1;

    private static final PlanCache PLANS = new PlanCache();
    private static volatile TerrainSampler sampler;
    private static final AtomicLong DENSITY_NANOS = new AtomicLong(), DENSITY_FIELDS = new AtomicLong();

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new EndTerrain());
    }

    /** The world seed: HEE changes World#getSeed in the End, so read the overworld's world info. */
    public static Long seed() {
        World overworld = DimensionManager.getWorld(0);
        return overworld == null ? null
            : overworld.getWorldInfo()
                .getSeed();
    }

    public static TerrainSampler sampler(long seed) {
        TerrainSampler s = sampler;
        if (s == null || s.seed() != seed) {
            s = new TerrainSampler(seed, new RegionPicker(Styles.all()));
            sampler = s;
        }
        return s;
    }

    /** The plan for a chunk, built once and shared by the three passes. */
    public static ChunkPlan plan(IChunkProvider generator, World end, long seed, int chunkX, int chunkZ) {
        TerrainSampler s = sampler(seed);
        return PLANS
            .get(seed, chunkX, chunkZ, () -> new ChunkPlan(s, chunkX, chunkZ, reservedLookup(generator, end, seed)));
    }

    private static ChunkPlan.ReservedLookup reservedLookup(IChunkProvider generator, World end, long seed) {
        return (cx, cz) -> Reservations.forChunk(generator, end, seed, cx, cz);
    }

    /** Average microseconds per density field (plan included) since start, and how many fields were built. */
    public static long[] densityStats() {
        long fields = DENSITY_FIELDS.get();
        return new long[] { fields == 0 ? 0 : DENSITY_NANOS.get() / fields / 1000, fields };
    }

    private static boolean outside(int chunkX, int chunkZ) {
        return Math.hypot(chunkX * 16 + 8, chunkZ * 16 + 8) >= TERRAIN_START;
    }

    @SubscribeEvent
    public void onInitNoiseField(ChunkProviderEvent.InitNoiseField event) {
        if (!(event.chunkProvider instanceof ChunkProviderEnd)) return;
        if (event.sizeX != DensityBuilder.SIZE_X || event.sizeY != DensityBuilder.SIZE_Y
            || event.sizeZ != DensityBuilder.SIZE_Z) return;
        int chunkX = event.posX / 2, chunkZ = event.posZ / 2;
        if (!outside(chunkX, chunkZ)) return;
        Long seed = seed();
        if (seed == null) return;
        double[] field = event.noisefield != null
            && event.noisefield.length == DensityBuilder.SIZE_X * DensityBuilder.SIZE_Y * DensityBuilder.SIZE_Z
                ? event.noisefield
                : new double[DensityBuilder.SIZE_X * DensityBuilder.SIZE_Y * DensityBuilder.SIZE_Z];
        long start = System.nanoTime();
        ChunkPlan plan = plan(event.chunkProvider, DimensionManager.getWorld(END), seed, chunkX, chunkZ);
        // The plan keeps the grid: the block pass reads it again to roughen steep faces.
        System.arraycopy(plan.densityField(false, true), 0, field, 0, field.length);
        DENSITY_NANOS.addAndGet(System.nanoTime() - start);
        DENSITY_FIELDS.incrementAndGet();
        event.noisefield = field;
        event.setResult(Event.Result.DENY);
    }

    @SubscribeEvent
    public void onReplaceBiomeBlocks(ChunkProviderEvent.ReplaceBiomeBlocks event) {
        if (event.world == null || event.world.provider.dimensionId != END || event.blockArray.length != 16 * 16 * 128)
            return;
        if (!outside(event.chunkX, event.chunkZ)) return;
        Long seed = seed();
        if (seed == null) return;
        ChunkPlan plan = plan(event.chunkProvider, event.world, seed, event.chunkX, event.chunkZ);
        plan.blocks(new McBlockSink(event.blockArray, event.metaArray, event.chunkX, event.chunkZ));
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        Chunk chunk = event.getChunk();
        World world = chunk.worldObj;
        if (world.isRemote || world.provider.dimensionId != END || chunk.isTerrainPopulated) return;
        if (!outside(chunk.xPosition, chunk.zPosition)) return;
        Long seed = seed();
        if (seed == null) return;
        ExtendedBlockStorage[] storage = chunk.getBlockStorageArray();
        for (int i = TallPass.BASE_Y >> 4; i < storage.length; i++) {
            if (storage[i] != null) return;
        }
        IChunkProvider generator = ((WorldServer) world).theChunkProviderServer.currentChunkProvider;
        ChunkPlan plan = plan(generator, world, seed, chunk.xPosition, chunk.zPosition);
        McStorageSink upper = new McStorageSink(storage, !world.provider.hasNoSky, chunk.xPosition, chunk.zPosition);
        if (TallPass.needed(plan)) {
            int ox = chunk.xPosition * 16, oz = chunk.zPosition * 16;
            TallPass.forEachSolid(
                TallPass.upperField(plan),
                (lx, y, lz) -> { if (y <= 255) upper.put(ox + lx, y, oz + lz, Blocks.end_stone, 0); });
        }
        // The same block work as in the generator's array, for Y 128-255.
        plan.blocks(upper);
        if (upper.wrote()) {
            // generateHeightMap is client-only in 1.7.10; generateSkylightMap also rebuilds the height map.
            chunk.generateSkylightMap();
            chunk.isModified = true;
        }
    }
}
