package chlorine.etjourney.world.end.feature.plant;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Recently built plant shapes of one world seed, failed builds included; a new seed clears it. */
final class ShapeCache<T> {

    private static final Object NONE = new Object();
    private final Map<Long, Object> map = new LinkedHashMap<Long, Object>(16, 0.75f, true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Object> e) {
            return size() > 512;
        }
    };
    private long cachedSeed;
    private boolean seeded;

    /** The shape under the key, built when missing; null when the build gave none. */
    @SuppressWarnings("unchecked")
    T get(long seed, long key, Supplier<T> build) {
        Object shape;
        synchronized (this) {
            if (!seeded || seed != cachedSeed) {
                map.clear();
                cachedSeed = seed;
                seeded = true;
            }
            shape = map.get(key);
        }
        if (shape == null) {
            T built = build.get();
            shape = built == null ? NONE : built;
            synchronized (this) {
                map.put(key, shape);
            }
        }
        return shape == NONE ? null : (T) shape;
    }

}
