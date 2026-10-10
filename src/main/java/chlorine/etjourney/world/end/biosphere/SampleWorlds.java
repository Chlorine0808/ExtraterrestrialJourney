package chlorine.etjourney.world.end.biosphere;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;

import chlorine.etjourney.core.config.ETJConfig;
import chlorine.etjourney.core.util.ModLog;
import chlorine.etjourney.world.end.feature.BiosphereSource;

/** The hidden dimensions biosphere cut-outs are generated in. */
public final class SampleWorlds {

    /** Sample dimensions that failed to load, and the server run that saw it. */
    private static final Set<Integer> BROKEN = new HashSet<>();
    private static MinecraftServer brokenIn;

    private SampleWorlds() {}

    public static void register() {
        DimensionManager.registerProviderType(ETJConfig.sampleDimension, SampleProvider.class, false);
        DimensionManager.registerDimension(ETJConfig.sampleDimension, ETJConfig.sampleDimension);
        DimensionManager.registerProviderType(ETJConfig.netherSampleDimension, NetherSampleProvider.class, false);
        DimensionManager.registerDimension(ETJConfig.netherSampleDimension, ETJConfig.netherSampleDimension);
        MinecraftForge.TERRAIN_GEN_BUS.register(new SampleStructures());
    }

    /**
     * The sample dimension for a kind of biosphere, loading it on first use; null once loading it failed during
     * this server's run.
     */
    public static WorldServer world(BiosphereSource.Kind kind) {
        int dim = kind == BiosphereSource.Kind.NETHER ? ETJConfig.netherSampleDimension : ETJConfig.sampleDimension;
        MinecraftServer server = MinecraftServer.getServer();
        if (server != brokenIn) {
            BROKEN.clear();
            brokenIn = server;
        }
        if (BROKEN.contains(dim)) return null;
        WorldServer w = DimensionManager.getWorld(dim);
        if (w != null) return w;
        try {
            DimensionManager.initDimension(dim);
            w = DimensionManager.getWorld(dim);
        } catch (RuntimeException | LinkageError e) {
            ModLog.LOG
                .error("Biosphere sample dimension {} could not be loaded; balls are cut from the other one", dim, e);
            w = null;
        }
        if (w == null) {
            // A world registered before its loading failed would be ticked, and crash the server.
            if (DimensionManager.getWorld(dim) != null) DimensionManager.setWorld(dim, null);
            BROKEN.add(dim);
        }
        return w;
    }

    /** The first kind, of the ball's own and its stand-in, whose sample dimension loads; null when neither does. */
    public static BiosphereSource.Kind usable(BiosphereSource.Kind kind) {
        for (BiosphereSource.Kind k : BiosphereSource.order(kind)) {
            if (world(k) != null) return k;
        }
        return null;
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
