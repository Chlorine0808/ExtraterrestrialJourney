package chlorine.etjourney.world.atmosphere;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayerMP;

import chlorine.etjourney.core.network.ETJNetwork;
import chlorine.etjourney.world.end.EndTerrain;
import chlorine.etjourney.world.end.TerrainSampler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;

/**
 * Server side: sends each End player the atmosphere grid around them whenever they enter a new cell. The client
 * never learns the seed this way.
 */
public final class AtmosphereTracker {

    private static final int END = EndTerrain.DIMENSION;
    private static final int CHECK_EVERY = 5, CACHE_LIMIT = 4096;

    private final CellWatch watch = new CellWatch();
    private final Map<Long, Byte> points = new LinkedHashMap<Long, Byte>(256, 0.75f, true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Byte> eldest) {
            return size() > CACHE_LIMIT;
        }
    };
    private long cachedSeed;

    private AtmosphereTracker() {}

    public static void register() {
        ETJNetwork.channel()
            .registerMessage(AtmosphereSync.Handler.class, AtmosphereSync.class, ETJNetwork.nextId(), Side.CLIENT);
        FMLCommonHandler.instance()
            .bus()
            .register(new AtmosphereTracker());
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        if (player.ticksExisted % CHECK_EVERY != 0) return;
        int cx = AtmosphereGrid.cellOf(player.posX), cz = AtmosphereGrid.cellOf(player.posZ);
        if (!watch.moved(player.getUniqueID(), player.dimension, cx, cz) || player.dimension != END) return;
        Long seed = EndTerrain.seed();
        if (seed == null) return;
        AtmosphereGrid grid = AtmosphereGrid.around(player.posX, player.posZ, sampler(seed));
        ETJNetwork.channel()
            .sendTo(new AtmosphereSync(END, grid), player);
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        watch.forget(event.player.getUniqueID());
    }

    /** Points depend only on the seed, so they are cached until the seed changes (a new world). */
    private AtmosphereGrid.PointSampler sampler(long seed) {
        if (seed != cachedSeed) {
            points.clear();
            cachedSeed = seed;
        }
        TerrainSampler terrain = EndTerrain.sampler(seed);
        return (x, z) -> {
            long key = (long) x << 32 | z & 0xFFFFFFFFL;
            Byte id = points.get(key);
            if (id == null) {
                id = Atmospheres.idOf(EndTerrain.inTerrain(x, z) ? terrain.biome(x, z) : null);
                points.put(key, id);
            }
            return id;
        };
    }
}
