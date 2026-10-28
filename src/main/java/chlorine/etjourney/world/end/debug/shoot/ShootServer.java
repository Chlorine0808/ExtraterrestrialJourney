package chlorine.etjourney.world.end.debug.shoot;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.common.DimensionManager;

import chlorine.etjourney.world.end.EndTerrain;
import chlorine.etjourney.world.end.TerrainSampler;
import chlorine.etjourney.world.end.biosphere.SampleWorlds;
import chlorine.etjourney.world.end.debug.EtjArgs;
import chlorine.etjourney.world.end.feature.Biosphere;
import chlorine.etjourney.world.end.feature.BiosphereSource;
import chlorine.etjourney.world.end.modifier.builtin.StructureModifiers;
import chlorine.etjourney.world.end.shoot.Framing;
import chlorine.etjourney.world.end.shoot.PlannedShot;
import chlorine.etjourney.world.end.shoot.Pocket;
import chlorine.etjourney.world.end.shoot.ShotList;
import chlorine.etjourney.world.end.shoot.ShotPlanner;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Server side of the screenshot run: jobs run one per server tick, shot list expansion and chunk readiness. */
public final class ShootServer {

    private static final int END = 1;
    /** Blocks per ray trace piece; rayTraceBlocks gives up after 200 block steps. */
    private static final int RAY_STEP = 48;
    private static final Queue<Runnable> JOBS = new ConcurrentLinkedQueue<>();
    /** Chunks this run loaded with no player near, packed as cx << 32 | cz; released after each shot. */
    private static final Set<Long> FORCED = new HashSet<>();
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
        WorldServer end = end();
        return ShotPlanner.expand(list, EndTerrain.sampler(seed), b -> {
            // A ball that gives way to a reserved island (HEE) is never built; it would leave an empty frame.
            if (!stands(end, seed, b)) return null;
            SampleWorlds.Site s = SampleWorlds.site(seed, b);
            if (s == null) return null;
            int x = s.point[0], z = s.point[1];
            boolean water = s.kind == BiosphereSource.Kind.SURFACE && SampleWorlds.isWater(s.world, x, z);
            return new ShotPlanner.Site(s.kind, x, z, water);
        });
    }

    /** Whether the End builds ball b, as BiosphereTransplant decides it; true before the End is loaded. */
    private static boolean stands(WorldServer end, long seed, Biosphere b) {
        if (end == null) return true;
        int cx = (int) Math.floor(b.centreX) >> 4, cz = (int) Math.floor(b.centreZ) >> 4;
        IChunkProvider generator = end.theChunkProviderServer.currentChunkProvider;
        return StructureModifiers.stands(EndTerrain.plan(generator, end, seed, cx, cz), b, Biosphere.KIND);
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

    /** The End, or null before it is loaded. Server thread only, like everything below. */
    static WorldServer end() {
        return DimensionManager.getWorld(END);
    }

    /** The first player on the server, or null. */
    static EntityPlayerMP player() {
        List<?> players = MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList;
        return players.isEmpty() ? null : (EntityPlayerMP) players.get(0);
    }

    /**
     * Loads up to budget missing End chunks within radius + 1 chunks of (x, z), so that those within radius get
     * populated even when no player is near, and says whether they all are ready.
     */
    public static boolean ensure(double x, double z, int radius, int budget) {
        WorldServer end = end();
        if (end == null) return false;
        int ccx = (int) Math.floor(x) >> 4, ccz = (int) Math.floor(z) >> 4;
        for (int cx = ccx - radius - 1; cx <= ccx + radius + 1 && budget > 0; cx++) {
            for (int cz = ccz - radius - 1; cz <= ccz + radius + 1 && budget > 0; cz++) {
                if (end.theChunkProviderServer.chunkExists(cx, cz)) continue;
                end.theChunkProviderServer.loadChunk(cx, cz);
                FORCED.add((long) cx << 32 | (cz & 0xFFFFFFFFL));
                budget--;
            }
        }
        return ready(x, z, radius);
    }

    /** Queues for unloading the chunks ensure loaded that lie beyond view chunks of the player. */
    public static void release(int view) {
        WorldServer end = end();
        EntityPlayerMP p = player();
        if (end == null || p == null) return;
        int pcx = (int) Math.floor(p.posX) >> 4, pcz = (int) Math.floor(p.posZ) >> 4;
        for (Iterator<Long> it = FORCED.iterator(); it.hasNext();) {
            long key = it.next();
            int cx = (int) (key >> 32), cz = (int) key;
            if (Math.max(Math.abs(cx - pcx), Math.abs(cz - pcz)) <= view + 1) continue;
            end.theChunkProviderServer.unloadChunksIfNotNearSpawn(cx, cz);
            it.remove();
        }
    }

    /** The ball a biosphere target was made from, found again by its key, or null. */
    public static Biosphere ball(TerrainSampler sampler, double x, double z, long key) {
        for (Biosphere b : Biosphere.KIND.near(sampler.seed(), x, z, 0, sampler.structureProbe())) {
            if (b.key() == key) return b;
        }
        return null;
    }

    /** The air inside the ball, read once from the End; outside the box and every non-INSIDE block is solid. */
    public static BallGrid grid(Biosphere b) {
        return new BallGrid(b, end());
    }

    /** A snapshot of a ball's inside air, with how much of the inside is air. */
    public static final class BallGrid implements Pocket.Grid {

        public final int x0, y0, z0, x1, y1, z1;
        /** Air blocks inside, and all blocks inside. */
        public final int air, inside;
        private final boolean[] cells, soft;

        BallGrid(Biosphere b, WorldServer end) {
            x0 = (int) Math.floor(b.centreX - b.radius);
            y0 = (int) Math.floor(b.y - b.radius);
            z0 = (int) Math.floor(b.centreZ - b.radius);
            x1 = (int) Math.floor(b.centreX + b.radius);
            y1 = (int) Math.floor(b.y + b.radius);
            z1 = (int) Math.floor(b.centreZ + b.radius);
            cells = new boolean[(x1 - x0 + 1) * (y1 - y0 + 1) * (z1 - z0 + 1)];
            soft = new boolean[cells.length];
            int nAir = 0, nInside = 0;
            for (int x = x0; x <= x1; x++) {
                for (int y = y0; y <= y1; y++) {
                    for (int z = z0; z <= z1; z++) {
                        if (b.part(x, y, z) != Biosphere.Part.INSIDE) continue;
                        nInside++;
                        if (!end.isAirBlock(x, y, z)) {
                            Block block = end.getBlock(x, y, z);
                            soft[index(x, y, z)] = block.isLeaves(end, x, y, z) || block.isWood(end, x, y, z)
                                || block.getMaterial() == Material.lava;
                            continue;
                        }
                        nAir++;
                        cells[index(x, y, z)] = true;
                    }
                }
            }
            air = nAir;
            inside = nInside;
        }

        @Override
        public boolean air(int x, int y, int z) {
            if (x < x0 || x > x1 || y < y0 || y > y1 || z < z0 || z > z1) return false;
            return cells[index(x, y, z)];
        }

        @Override
        public boolean noFooting(int x, int y, int z) {
            if (x < x0 || x > x1 || y < y0 || y > y1 || z < z0 || z > z1) return false;
            return soft[index(x, y, z)];
        }

        private int index(int x, int y, int z) {
            return ((x - x0) * (y1 - y0 + 1) + (y - y0)) * (z1 - z0 + 1) + (z - z0);
        }
    }

    /**
     * Feet height in column (x, z), from 48 below y to 24 above it, that stands on ground (not a tree crown) with
     * air at the feet and the head, the one nearest y; NaN when there is none.
     */
    public static double floor(int x, double y, int z) {
        WorldServer end = end();
        int lo = Math.max(1, (int) Math.floor(y) - 48), hi = Math.min(254, (int) Math.floor(y) + 24);
        double[] feet = new double[hi - lo + 1];
        for (int at = lo; at <= hi; at++) {
            Block below = end.getBlock(x, at - 1, z);
            boolean ground = !end.isAirBlock(x, at - 1, z) && !below.isLeaves(end, x, at - 1, z)
                && !below.isWood(end, x, at - 1, z);
            feet[at - lo] = ground && end.isAirBlock(x, at, z) && end.isAirBlock(x, at + 1, z) ? at : Double.NaN;
        }
        int i = Framing.pickGround(y, feet);
        return i < 0 ? Double.NaN : feet[i];
    }

    /** Whether the block at (x, y, z) is air. */
    public static boolean air(double x, double y, double z) {
        return end().isAirBlock((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    /**
     * Distance from eye along dir to the first block with a collision box within range, or NaN when there is none.
     * The look is traced in pieces because one trace stops after 200 block steps.
     */
    public static double firstHit(double[] eye, double[] dir, int range) {
        WorldServer end = end();
        for (double[] s : EtjArgs.lookSegments(eye, dir, range, RAY_STEP)) {
            Vec3 from = Vec3.createVectorHelper(s[0], s[1], s[2]);
            Vec3 to = Vec3.createVectorHelper(s[3], s[4], s[5]);
            MovingObjectPosition hit = end.func_147447_a(from, to, false, true, false);
            if (hit != null && hit.hitVec != null) {
                double dx = hit.hitVec.xCoord - eye[0], dy = hit.hitVec.yCoord - eye[1];
                double dz = hit.hitVec.zCoord - eye[2];
                return Math.sqrt(dx * dx + dy * dy + dz * dz);
            }
        }
        return Double.NaN;
    }
}
