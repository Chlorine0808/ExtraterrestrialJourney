package chlorine.etjourney.world.end.biosphere;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
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

    /** Sample dimensions registered at start. */
    private static final Set<Integer> REGISTERED = new HashSet<>();
    /** Sample dimensions that failed to load, and the server run that saw it (weakly, so it can be collected). */
    private static final Set<Integer> BROKEN = new HashSet<>();
    private static WeakReference<MinecraftServer> brokenIn = new WeakReference<>(null);

    private SampleWorlds() {}

    public static void register() {
        register(ETJConfig.sampleDimension, SampleProvider.class);
        register(ETJConfig.netherSampleDimension, NetherSampleProvider.class);
        MinecraftForge.TERRAIN_GEN_BUS.register(new SampleStructures());
    }

    /**
     * Registers a sample dimension. An ID another mod holds stops the game here, with a message saying what to
     * change: going on without the dimension would leave biospheres quietly missing.
     */
    private static void register(int dim, Class<? extends WorldProvider> provider) {
        if (DimensionManager.isDimensionRegistered(dim)
            || !DimensionManager.registerProviderType(dim, provider, false)) {
            throw new IllegalStateException(
                "Extraterrestrial Journey: biosphere sample dimension " + dim
                    + " is already taken by another mod. Set sampleDimension and netherSampleDimension in"
                    + " config/etjourney.cfg (section biospheres) to free dimension IDs.");
        }
        DimensionManager.registerDimension(dim, dim);
        REGISTERED.add(dim);
    }

    /**
     * The sample dimension for a kind of biosphere, loading it on first use; null once loading it failed during
     * this server's run.
     */
    public static WorldServer world(BiosphereSource.Kind kind) {
        int dim = kind == BiosphereSource.Kind.NETHER ? ETJConfig.netherSampleDimension : ETJConfig.sampleDimension;
        if (!REGISTERED.contains(dim)) return null;
        MinecraftServer server = MinecraftServer.getServer();
        if (server != brokenIn.get()) {
            BROKEN.clear();
            brokenIn = new WeakReference<>(server);
        }
        if (BROKEN.contains(dim)) return null;
        WorldServer w = DimensionManager.getWorld(dim);
        if (w != null) return w;
        try {
            DimensionManager.initDimension(dim);
            w = DimensionManager.getWorld(dim);
        } catch (Throwable e) {
            rethrowFatal(e);
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

    /**
     * Lets through only what must not be swallowed: a dying thread or a JVM out of memory. Anything else thrown
     * by another mod's generation, a stack overflow included, has unwound by the time it is caught.
     */
    static void rethrowFatal(Throwable t) {
        if (t instanceof ThreadDeath) throw (ThreadDeath) t;
        if (t instanceof VirtualMachineError && !(t instanceof StackOverflowError)) throw (VirtualMachineError) t;
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
