package chlorine.etjourney.compat.hee;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.World;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.MapGenBase;

import chlorine.etjourney.core.util.ModLog;
import chlorine.etjourney.world.end.reserve.Area;
import chlorine.etjourney.world.end.reserve.ReservationProvider;

/**
 * Asks HEE's own island structure generator which chunks start an island, so our terrain fades around them.
 * An HEE island occupies 208 blocks east and south of its start chunk's corner.
 */
final class HeeIslandReservations implements ReservationProvider {

    private static final int ISLAND_SIZE = 208;
    private static final double RADIUS = ISLAND_SIZE / 2.0 + 24;

    private final Map<Long, Boolean> cache = new ConcurrentHashMap<>();
    private Object islandGen;
    private Method canSpawn;
    private boolean broken;
    /** The End world the binding belongs to; a new world (another save) rebinds and drops the cache. */
    private WeakReference<World> boundWorld = new WeakReference<>(null);

    @Override
    public List<Area> near(Object generator, Object world, int chunkX, int chunkZ, int chunkRadius) {
        List<Area> out = new ArrayList<>();
        if (broken || !(world instanceof World) || !bind(generator, (World) world)) return out;
        // Starts west or north of the chunk can still reach it, because islands extend east and south.
        int back = ISLAND_SIZE / 16 + 1;
        for (int cx = chunkX - chunkRadius - back; cx <= chunkX + chunkRadius; cx++) {
            for (int cz = chunkZ - chunkRadius - back; cz <= chunkZ + chunkRadius; cz++) {
                if (startsIn(cx, cz)) {
                    out.add(new Area("hee", cx * 16 + ISLAND_SIZE / 2.0, cz * 16 + ISLAND_SIZE / 2.0, RADIUS));
                }
            }
        }
        return out;
    }

    private synchronized boolean bind(Object generator, World end) {
        if (islandGen != null && boundWorld.get() == end) return true;
        islandGen = null;
        cache.clear();
        try {
            Object provider = generator != null && generator.getClass()
                .getName()
                .equals("chylex.hee.world.ChunkProviderHardcoreEnd")
                    ? generator
                    : end.getChunkProvider() instanceof ChunkProviderServer
                        ? ((ChunkProviderServer) end.getChunkProvider()).currentChunkProvider
                        : null;
            if (provider == null || !provider.getClass()
                .getName()
                .equals("chylex.hee.world.ChunkProviderHardcoreEnd")) return false;
            Field gen = provider.getClass()
                .getDeclaredField("islandGen");
            gen.setAccessible(true);
            Object structure = gen.get(provider);
            Field worldField = findField(MapGenBase.class, "worldObj", "field_75039_c");
            if (worldField.get(structure) == null) worldField.set(structure, end);
            canSpawn = findMethod(structure.getClass(), "canSpawnStructureAtCoords", "func_75047_a");
            islandGen = structure;
            boundWorld = new WeakReference<>(end);
            ModLog.LOG.info("Reserving room for HEE islands in the End terrain");
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            broken = true;
            ModLog.LOG.warn("Could not query HEE islands; not reserving space: {}", e.toString());
            return false;
        }
    }

    private boolean startsIn(int cx, int cz) {
        long key = ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
        Boolean hit = cache.get(key);
        if (hit == null) {
            try {
                hit = (Boolean) canSpawn.invoke(islandGen, cx, cz);
            } catch (ReflectiveOperationException e) {
                hit = Boolean.FALSE;
            }
            if (cache.size() > 65536) cache.clear();
            cache.put(key, hit);
        }
        return hit;
    }

    private static Field findField(Class<?> owner, String... names) throws NoSuchFieldException {
        for (String name : names) {
            try {
                Field f = owner.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException(String.join("/", names));
    }

    private static Method findMethod(Class<?> type, String... names) throws NoSuchMethodException {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            for (String name : names) {
                try {
                    Method m = c.getDeclaredMethod(name, int.class, int.class);
                    m.setAccessible(true);
                    return m;
                } catch (NoSuchMethodException ignored) {}
            }
        }
        throw new NoSuchMethodException(String.join("/", names));
    }
}
