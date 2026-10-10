package chlorine.etjourney.world.end.biosphere;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;

import chlorine.etjourney.core.util.ModLog;
import chlorine.etjourney.world.end.ChunkPlan;
import chlorine.etjourney.world.end.EndTerrain;
import chlorine.etjourney.world.end.feature.Biosphere;
import chlorine.etjourney.world.end.feature.Cutout;
import chlorine.etjourney.world.end.feature.CutoutCache;
import chlorine.etjourney.world.end.modifier.builtin.StructureModifiers;
import cpw.mods.fml.common.IWorldGenerator;
import cpw.mods.fml.common.registry.GameRegistry;

/** Fills each biosphere at populate time, window by window, from the terrain cut for it. */
public final class BiosphereTransplant implements IWorldGenerator {

    private static final int END = 1;
    /** Cut-outs kept at once; each is about 1.7 MB. */
    private static final int CACHED = 16;

    private final CutoutCache cache = new CutoutCache(CACHED);

    public static void register() {
        SampleWorlds.register();
        GameRegistry.registerWorldGenerator(new BiosphereTransplant(), 10);
    }

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider generator,
        IChunkProvider provider) {
        if (world.isRemote || world.provider.dimensionId != END) return;
        int ox = chunkX * 16 + 8, oz = chunkZ * 16 + 8;
        if (Math.hypot(ox + 8, oz + 8) < EndTerrain.TERRAIN_START) return;
        Long seed = EndTerrain.seed();
        if (seed == null) return;
        cache.forSeed(seed);
        ChunkPlan plan = null;
        for (Biosphere b : Biosphere.KIND.near(
            seed,
            ox + 8,
            oz + 8,
            Biosphere.WINDOW_REACH,
            EndTerrain.sampler(seed)
                .structureProbe())) {
            if (!b.touchesWindow(chunkX, chunkZ)) continue;
            if (plan == null) plan = EndTerrain.plan(generator, world, seed, chunkX, chunkZ);
            // A ball that gives way to a reserved island has no shell; leave it out as the block pass does.
            if (!StructureModifiers.stands(plan, b, Biosphere.KIND)) continue;
            if (cache.failed(b.key())) {
                erase(world, b, ox, oz);
                continue;
            }
            Cutout cut = cache.get(b.key());
            if (cut == null) {
                cut = CutoutMaker.make(seed, b);
                if (cut == null) {
                    // Neither world could make it: take the shell away rather than leave an empty ball.
                    cache.fail(b.key());
                    erase(world, b, ox, oz);
                    continue;
                }
                cache.put(b.key(), cut);
            }
            copy(world, b, cut, ox, oz);
            if (cut.windowDone()) cache.remove(b.key());
        }
    }

    private static void copy(World world, Biosphere b, Cutout cut, int ox, int oz) {
        int y0 = Math.max(0, (int) Math.floor(b.minY())), y1 = Math.min(255, (int) Math.ceil(b.maxY()));
        int failed = 0;
        RuntimeException first = null;
        for (int x = ox; x < ox + 16; x++) {
            for (int z = oz; z < oz + 16; z++) {
                for (int y = y0; y <= y1; y++) {
                    if (b.part(x, y, z) != Biosphere.Part.INSIDE) continue;
                    int packed = cut.get(x, y, z);
                    try {
                        if (packed == 0) {
                            // Liquid flowing in from a window copied earlier, where the sample has air.
                            if (!world.isAirBlock(x, y, z)) world.setBlock(x, y, z, Blocks.air, 0, 2);
                            continue;
                        }
                        Block block = Block.getBlockById(packed >> 4);
                        if (block != null) world.setBlock(x, y, z, block, packed & 15, 2);
                    } catch (RuntimeException e) {
                        // A mod's block failing as it is placed costs that block, not the End.
                        if (first == null) first = e;
                        failed++;
                    }
                }
            }
        }
        if (first != null) ModLog.LOG.warn(
            "{} blocks of the biosphere at {},{} could not be placed",
            failed,
            (int) b.centreX,
            (int) b.centreZ,
            first);
    }

    /** Takes the shell of a ball that could not be filled out of the window. */
    private static void erase(World world, Biosphere b, int ox, int oz) {
        int y0 = Math.max(0, (int) Math.floor(b.minY())), y1 = Math.min(255, (int) Math.ceil(b.maxY()));
        for (int x = ox; x < ox + 16; x++) {
            for (int z = oz; z < oz + 16; z++) {
                for (int y = y0; y <= y1; y++) {
                    if (b.part(x, y, z) == Biosphere.Part.GLASS && !world.isAirBlock(x, y, z))
                        world.setBlock(x, y, z, Blocks.air, 0, 2);
                }
            }
        }
    }
}
