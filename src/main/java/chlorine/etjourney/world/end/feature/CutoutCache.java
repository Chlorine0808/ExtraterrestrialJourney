package chlorine.etjourney.world.end.feature;

import java.util.LinkedHashMap;
import java.util.Map;

/** Cut-outs by biosphere key, dropping the least recently used beyond a capacity, and balls that failed. */
public final class CutoutCache {

    private final Map<Long, Cutout> map;
    /** Balls whose sample could not be generated, kept so that later windows do not try again. */
    private final Map<Long, Boolean> failures;
    private Long seed;

    public CutoutCache(int capacity) {
        map = new LinkedHashMap<Long, Cutout>(16, 0.75f, true) {

            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, Cutout> eldest) {
                return size() > capacity;
            }
        };
        failures = new LinkedHashMap<Long, Boolean>(16, 0.75f, true) {

            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, Boolean> eldest) {
                return size() > capacity;
            }
        };
    }

    /** Forgets every cut-out and failure when the seed differs from the last one seen, as after a world switch. */
    public void forSeed(long seed) {
        if (this.seed != null && this.seed == seed) return;
        this.seed = seed;
        map.clear();
        failures.clear();
    }

    public void fail(long key) {
        failures.put(key, Boolean.TRUE);
    }

    public boolean failed(long key) {
        return failures.containsKey(key);
    }

    public Cutout get(long key) {
        return map.get(key);
    }

    public void put(long key, Cutout cut) {
        map.put(key, cut);
    }

    public void remove(long key) {
        map.remove(key);
    }

    public int size() {
        return map.size();
    }
}
