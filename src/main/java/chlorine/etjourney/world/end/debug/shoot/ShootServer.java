package chlorine.etjourney.world.end.debug.shoot;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import chlorine.etjourney.world.end.EndTerrain;
import chlorine.etjourney.world.end.biosphere.SampleWorlds;
import chlorine.etjourney.world.end.feature.BiosphereSource;
import chlorine.etjourney.world.end.shoot.PlannedShot;
import chlorine.etjourney.world.end.shoot.ShotList;
import chlorine.etjourney.world.end.shoot.ShotPlanner;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Server side of the screenshot run: jobs run one per server tick, shot list expansion and chunk readiness. */
public final class ShootServer {

    private static final int END = 1;
    private static final Queue<Runnable> JOBS = new ConcurrentLinkedQueue<>();
    private static boolean registered;

    private ShootServer() {}

    /** Registers the tick handler; called only from the etj.shoot-gated start. */
    public static synchronized void register() {
        if (registered) return;
        FMLCommonHandler.instance()
            .bus()
            .register(new ShootServer());
        registered = true;
    }

    /** Runs job on the server thread at the end of a later tick, one job per tick. */
    public static <T> CompletableFuture<T> submit(Supplier<T> job) {
        CompletableFuture<T> future = new CompletableFuture<>();
        JOBS.add(() -> {
            try {
                future.complete(job.get());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Runnable job = JOBS.poll();
        if (job != null) job.run();
    }

    /** The planned shots of the list on this world's seed; call on the server thread (sample worlds load). */
    public static List<PlannedShot> expand(ShotList list) {
        long seed = seed(list);
        return ShotPlanner.expand(list, EndTerrain.sampler(seed), b -> {
            SampleWorlds.Site s = SampleWorlds.site(seed, b);
            if (s == null) return null;
            int x = s.point[0], z = s.point[1];
            boolean water = s.kind == BiosphereSource.Kind.SURFACE && SampleWorlds.isWater(s.world, x, z);
            return new ShotPlanner.Site(s.kind, x, z, water);
        });
    }

    /** The world's seed, or the list's before the overworld is up. */
    static long seed(ShotList list) {
        Long seed = EndTerrain.seed();
        return seed == null ? list.seed : seed;
    }

    /** Whether every End chunk within radius chunks of (x, z) is loaded and populated. */
    public static boolean ready(double x, double z, int radius) {
        WorldServer end = DimensionManager.getWorld(END);
        if (end == null) return false;
        int ccx = (int) Math.floor(x) >> 4, ccz = (int) Math.floor(z) >> 4;
        for (int cx = ccx - radius; cx <= ccx + radius; cx++) {
            for (int cz = ccz - radius; cz <= ccz + radius; cz++) {
                if (!end.theChunkProviderServer.chunkExists(cx, cz)) return false;
                if (!end.getChunkFromChunkCoords(cx, cz).isTerrainPopulated) return false;
            }
        }
        return true;
    }
}
