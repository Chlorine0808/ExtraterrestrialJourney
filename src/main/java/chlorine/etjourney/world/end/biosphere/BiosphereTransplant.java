package chlorine.etjourney.world.end.biosphere;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;

import chlorine.etjourney.core.util.ModLog;
import chlorine.etjourney.world.end.EndPalette;
import chlorine.etjourney.world.end.EndTerrain;
import chlorine.etjourney.world.end.feature.Biosphere;
import chlorine.etjourney.world.end.feature.Cutout;
import chlorine.etjourney.world.end.feature.CutoutCache;
import chlorine.etjourney.world.end.modifier.EndBlock;
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
        for (Biosphere b : Biosphere.KIND.near(
            seed,
            ox + 8,
            oz + 8,
            Biosphere.WINDOW_REACH,
            EndTerrain.sampler(seed)
                .structureProbe())) {
            if (!b.touchesWindow(chunkX, chunkZ)) continue;
            try {
                fill(world, generator, seed, chunkX, chunkZ, b);
            } catch (Throwable t) {
                // A last guard: this feature must never take End generation down with it.
                SampleWorlds.rethrowFatal(t);
                ModLog.LOG.warn("Biosphere at {},{} failed while filling", (int) b.centreX, (int) b.centreZ, t);
            }
        }
    }

    /** Copies the part of a ball in the window of chunk (chunkX, chunkZ), making its cut-out on first use. */
    private void fill(World world, IChunkProvider generator, long seed, int chunkX, int chunkZ, Biosphere b) {
        int ox = chunkX * 16 + 8, oz = chunkZ * 16 + 8;
        // A ball that gives way to a reserved island has no shell; leave it out as the block pass does.
        if (!StructureModifiers.stands(EndTerrain.plan(generator, world, seed, chunkX, chunkZ), b, Biosphere.KIND))
            return;
        if (cache.failed(b.key())) {
            erase(world, b, ox, oz);
            return;
        }
        Cutout cut = cache.get(b.key());
        if (cut == null) {
            cut = CutoutMaker.make(seed, b);
            if (cut == null) {
                // Neither world could make it: take the shell away rather than leave an empty ball.
                cache.fail(b.key());
                erase(world, b, ox, oz);
                return;
            }
            cache.put(b.key(), cut);
        }
        copy(world, b, cut, ox, oz);
        if (cut.windowDone()) cache.remove(b.key());
    }

    private static void copy(World world, Biosphere b, Cutout cut, int ox, int oz) {
        int y0 = Math.max(0, (int) Math.floor(b.minY())), y1 = Math.min(255, (int) Math.ceil(b.maxY()));
        int failed = 0;
        Throwable first = null;
        for (int x = ox; x < ox + 16; x++) {
            for (int z = oz; z < oz + 16; z++) {
                for (int y = y0; y <= y1; y++) {
                    if (b.part(x, y, z) != Biosphere.Part.INSIDE) continue;
                    try {
                        place(world, cut, x, y, z);
                    } catch (Throwable t) {
                        // A mod's block failing as it is placed costs that block, not the End.
                        SampleWorlds.rethrowFatal(t);
                        if (first == null) first = t;
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

    private static void place(World world, Cutout cut, int x, int y, int z) {
        int packed = cut.get(x, y, z);
        if (packed == 0) {
            // Liquid flowing in from a window copied earlier, where the sample has air.
            if (!world.isAirBlock(x, y, z)) world.setBlock(x, y, z, Blocks.air, 0, 2);
            return;
        }
        Block block = Block.getBlockById(packed >> 4);
        if (block == null) return;
        world.setBlock(x, y, z, block, packed & 15, 2);
        Object data = cut.tile(x, y, z);
        if (!(data instanceof NBTTagCompound)) return;
        // The block's own tile entity, with the data the sample's generator gave it, at its new position.
        NBTTagCompound tag = (NBTTagCompound) ((NBTTagCompound) data).copy();
        tag.setInteger("x", x);
        tag.setInteger("y", y);
        tag.setInteger("z", z);
        TileEntity tile = TileEntity.createAndLoadEntity(tag);
        if (tile != null) world.setTileEntity(x, y, z, tile);
        else world.setBlock(x, y, z, Blocks.air, 0, 2);
    }

    /** Takes the shell of a ball that could not be filled out of the window, leaving other blocks alone. */
    private static void erase(World world, Biosphere b, int ox, int oz) {
        Block shell = EndPalette.block(EndBlock.GLASS);
        int y0 = Math.max(0, (int) Math.floor(b.minY())), y1 = Math.min(255, (int) Math.ceil(b.maxY()));
        for (int x = ox; x < ox + 16; x++) {
            for (int z = oz; z < oz + 16; z++) {
                for (int y = y0; y <= y1; y++) {
                    if (b.part(x, y, z) != Biosphere.Part.GLASS || world.getBlock(x, y, z) != shell) continue;
                    try {
                        world.setBlock(x, y, z, Blocks.air, 0, 2);
                    } catch (Throwable t) {
                        SampleWorlds.rethrowFatal(t);
                    }
                }
            }
        }
    }
}
