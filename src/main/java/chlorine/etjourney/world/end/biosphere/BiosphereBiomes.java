package chlorine.etjourney.world.end.biosphere;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkEvent;

import chlorine.etjourney.core.config.ETJConfig;
import chlorine.etjourney.core.util.ModLog;
import chlorine.etjourney.world.end.EndTerrain;
import chlorine.etjourney.world.end.feature.Biosphere;
import chlorine.etjourney.world.end.modifier.builtin.StructureModifiers;
import chlorine.etjourney.world.end.noise.Distance;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Gives each biosphere's columns its sample's biomes as the End chunk is made, so grass and leaves match. */
public final class BiosphereBiomes {

    private static final int END = 1;
    /** Balls whose site is kept; one ball spans about 25 chunks, made close together. */
    private static final int SITES = 64;
    /** Stands for a ball with no site in the map, which holds no nulls. */
    private static final SampleWorlds.Site NONE = new SampleWorlds.Site(null, null, null);

    private final Map<Long, SampleWorlds.Site> sites = new LinkedHashMap<Long, SampleWorlds.Site>(16, 0.75f, true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, SampleWorlds.Site> eldest) {
            return size() > SITES;
        }
    };
    private long sitesSeed;

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new BiosphereBiomes());
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (!ETJConfig.biomeColors) return;
        Chunk chunk = event.getChunk();
        World world = chunk.worldObj;
        // Only a chunk not yet populated, so the biomes are in place before it is ever sent to a client.
        if (world.isRemote || world.provider.dimensionId != END || chunk.isTerrainPopulated) return;
        int ox = chunk.xPosition * 16, oz = chunk.zPosition * 16;
        if (Distance.planar(ox + 8, oz + 8) < EndTerrain.TERRAIN_START) return;
        Long seed = EndTerrain.seed();
        if (seed == null) return;
        try {
            paint((WorldServer) world, chunk, seed);
        } catch (Throwable t) {
            // The colours are a nicety; a failure costs them, not the chunk.
            SampleWorlds.rethrowFatal(t);
            ModLog.LOG
                .warn("Biosphere biomes for chunk {},{} could not be written", chunk.xPosition, chunk.zPosition, t);
        }
    }

    private void paint(WorldServer end, Chunk chunk, long seed) {
        int ox = chunk.xPosition * 16, oz = chunk.zPosition * 16;
        IChunkProvider generator = end.theChunkProviderServer.currentChunkProvider;
        for (Biosphere b : Biosphere.KIND.near(
            seed,
            ox + 8,
            oz + 8,
            Biosphere.WINDOW_REACH,
            EndTerrain.sampler(seed)
                .structureProbe())) {
            // A ball that gives way to a reserved island has no shell and keeps the End's biomes.
            if (!StructureModifiers
                .stands(EndTerrain.plan(generator, end, seed, chunk.xPosition, chunk.zPosition), b, Biosphere.KIND))
                continue;
            SampleWorlds.Site site = site(seed, b);
            if (site == null) continue;
            int cx = (int) Math.floor(b.centreX), cz = (int) Math.floor(b.centreZ);
            // The same offset from the sample point as the cut-out uses for blocks.
            BiomeGenBase[] sample = site.world.getWorldChunkManager()
                .getBiomeGenAt(null, site.point[0] + ox - cx, site.point[1] + oz - cz, 16, 16, false);
            byte[] biomes = chunk.getBiomeArray();
            for (int lz = 0; lz < 16; lz++) {
                for (int lx = 0; lx < 16; lx++) {
                    BiomeGenBase biome = sample[lz * 16 + lx];
                    if (biome == null || !b.coversColumn(ox + lx, oz + lz)) continue;
                    biomes[lz << 4 | lx] = (byte) (biome.biomeID & 255);
                }
            }
            chunk.isModified = true;
        }
    }

    /** The ball's site, worked out once and kept while its sample world stays loaded. */
    private SampleWorlds.Site site(long seed, Biosphere b) {
        if (seed != sitesSeed) {
            sites.clear();
            sitesSeed = seed;
        }
        SampleWorlds.Site site = sites.get(b.key());
        if (site != null && site != NONE && DimensionManager.getWorld(site.world.provider.dimensionId) != site.world)
            site = null;
        if (site == null) {
            site = SampleWorlds.site(seed, b);
            if (site == null) site = NONE;
            sites.put(b.key(), site);
        }
        return site == NONE ? null : site;
    }
}
