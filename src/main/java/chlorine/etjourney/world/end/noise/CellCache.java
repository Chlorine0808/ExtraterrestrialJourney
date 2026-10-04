package chlorine.etjourney.world.end.noise;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Per-cell results for one world seed; cleared when the seed changes or the limit is passed. */
public final class CellCache<T> {

    public interface CellFunction<T> {

        T compute(long seed, int cx, int cz);
    }

    private static final Object NONE = new Object();

    private final int limit;
    private final Map<Long, Object> cells = new ConcurrentHashMap<>();
    private volatile long seed;
    private volatile boolean seeded;

    public CellCache(int limit) {
        this.limit = limit;
    }

    @SuppressWarnings("unchecked")
    public T get(long worldSeed, int cx, int cz, CellFunction<T> compute) {
        if (!seeded || worldSeed != seed) {
            cells.clear();
            seed = worldSeed;
            seeded = true;
        }
        long key = ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
        Object value = cells.get(key);
        if (value == null) {
            T computed = compute.compute(worldSeed, cx, cz);
            value = computed == null ? NONE : computed;
            if (cells.size() > limit) cells.clear();
            cells.put(key, value);
        }
        return value == NONE ? null : (T) value;
    }
}
