package chlorine.etjourney.world.end.biosphere;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraftforge.common.DimensionManager;

import chlorine.etjourney.world.end.EndTerrain;
import chlorine.etjourney.world.end.feature.Biosphere;
import chlorine.etjourney.world.end.feature.BiospherePrefetch;
import chlorine.etjourney.world.end.feature.Cutout;
import chlorine.etjourney.world.end.feature.CutoutCache;
import chlorine.etjourney.world.end.modifier.builtin.StructureModifiers;
import chlorine.etjourney.world.end.noise.Distance;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Cuts the biospheres ahead of the players in the End, a few milliseconds a tick, so populate finds them ready. */
final class CutoutPrefetch {

    private static final int END = 1;
    /** Time a tick may spend on the job in hand. */
    private static final long BUDGET_NANOS = 10_000_000L;
    /** Ticks between looks for the next ball to cut. */
    private static final int SCAN_TICKS = 20;
    /** Blocks beyond the generated area within which a ball is cut ahead. */
    private static final double MARGIN = 128;

    private final CutoutCache cache;
    private CutoutJob job;
    private long jobSeed;
    private int ticks;

    CutoutPrefetch(CutoutCache cache) {
        this.cache = cache;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Long seed = EndTerrain.seed();
        if (job != null && job.stale()) job = null;
        if (job != null) {
            if (seed == null || seed != jobSeed) {
                job.abort();
                job = null;
                return;
            }
            if (job.step(BUDGET_NANOS)) finish();
            return;
        }
        if (++ticks < SCAN_TICKS) return;
        ticks = 0;
        WorldServer end = DimensionManager.getWorld(END);
        if (end == null || seed == null || end.playerEntities.isEmpty()) return;
        cache.forSeed(seed);
        List<double[]> players = new ArrayList<>();
        for (Object o : end.playerEntities) {
            EntityPlayer player = (EntityPlayer) o;
            players.add(new double[] { player.posX, player.posZ });
        }
        double reach = MinecraftServer.getServer()
            .getConfigurationManager()
            .getViewDistance() * 16 + MARGIN;
        Biosphere b = BiospherePrefetch.pick(
            seed,
            players,
            reach,
            EndTerrain.sampler(seed)
                .structureProbe(),
            ball -> wanted(end, seed, ball));
        if (b == null) return;
        job = new CutoutJob(seed, b);
        jobSeed = seed;
    }

    /** For populate: the ball's cut-out, finishing a job on it now, or else stopping any other job and making it. */
    Cutout make(long seed, Biosphere b) {
        if (job != null && job.stale()) job = null;
        if (job != null && job.ball.key() == b.key() && jobSeed == seed) {
            job.step(Long.MAX_VALUE);
            Cutout cut = job.result();
            job = null;
            return cut;
        }
        if (job != null) {
            // A cut-out starts from an empty sample world; the stopped job is chosen again later.
            job.abort();
            job = null;
        }
        return CutoutMaker.make(seed, b);
    }

    private void finish() {
        long key = job.ball.key();
        Cutout cut = job.result();
        if (cut == null) cache.fail(key);
        else cache.put(key, cut);
        job = null;
    }

    /** A ball still to be cut whose chunks are not generated yet and whose shell will stand. */
    private boolean wanted(WorldServer end, long seed, Biosphere b) {
        long key = b.key();
        if (cache.get(key) != null || cache.failed(key)) return false;
        int cx = (int) Math.floor(b.centreX) >> 4, cz = (int) Math.floor(b.centreZ) >> 4;
        if (Distance.planar(cx * 16 + 8, cz * 16 + 8) < EndTerrain.TERRAIN_START) return false;
        if (generated(end, cx, cz)) return false;
        return StructureModifiers.stands(
            EndTerrain.plan(end.theChunkProviderServer.currentChunkProvider, end, seed, cx, cz),
            b,
            Biosphere.KIND);
    }

    private static boolean generated(WorldServer end, int cx, int cz) {
        if (end.theChunkProviderServer.chunkExists(cx, cz)) return true;
        return end.theChunkProviderServer.currentChunkLoader instanceof AnvilChunkLoader
            && ((AnvilChunkLoader) end.theChunkProviderServer.currentChunkLoader).chunkExists(end, cx, cz);
    }
}
