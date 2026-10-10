package chlorine.etjourney.world.end.biosphere;

import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;

import chlorine.etjourney.core.config.ETJConfig;
import chlorine.etjourney.world.end.feature.BiosphereSource;

/** The hidden dimensions biosphere cut-outs are generated in. */
public final class SampleWorlds {

    private SampleWorlds() {}

    public static void register() {
        DimensionManager.registerProviderType(ETJConfig.sampleDimension, SampleProvider.class, false);
        DimensionManager.registerDimension(ETJConfig.sampleDimension, ETJConfig.sampleDimension);
        DimensionManager.registerProviderType(ETJConfig.netherSampleDimension, NetherSampleProvider.class, false);
        DimensionManager.registerDimension(ETJConfig.netherSampleDimension, ETJConfig.netherSampleDimension);
        MinecraftForge.TERRAIN_GEN_BUS.register(new SampleStructures());
    }

    /** The sample dimension for a kind of biosphere, loading it on first use. */
    public static WorldServer world(BiosphereSource.Kind kind) {
        int dim = kind == BiosphereSource.Kind.NETHER ? ETJConfig.netherSampleDimension : ETJConfig.sampleDimension;
        WorldServer w = DimensionManager.getWorld(dim);
        if (w == null) {
            DimensionManager.initDimension(dim);
            w = DimensionManager.getWorld(dim);
        }
        return w;
    }

    public static boolean isSample(World w) {
        int dim = w.provider.dimensionId;
        return dim == ETJConfig.sampleDimension || dim == ETJConfig.netherSampleDimension;
    }

    /** Whether the biome map, without generating anything, puts an ocean, a river or a beach at (x, z). */
    public static boolean isWater(World w, int x, int z) {
        BiomeGenBase b = w.getWorldChunkManager()
            .getBiomeGenAt(x, z);
        return b != null && (BiomeDictionary.isBiomeOfType(b, BiomeDictionary.Type.OCEAN)
            || BiomeDictionary.isBiomeOfType(b, BiomeDictionary.Type.RIVER)
            || BiomeDictionary.isBiomeOfType(b, BiomeDictionary.Type.BEACH));
    }
}
