package chlorine.etjourney.world.end.feature;

import java.util.LinkedHashMap;
import java.util.Map;

/** Cut-outs by biosphere key, dropping the least recently used beyond a capacity. */
public final class CutoutCache {

    private final Map<Long, Cutout> map;

    public CutoutCache(int capacity) {
        map = new LinkedHashMap<Long, Cutout>(16, 0.75f, true) {

            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, Cutout> eldest) {
                return size() > capacity;
            }
        };
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
