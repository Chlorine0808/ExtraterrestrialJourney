package chlorine.etjourney.world.end.feature;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Names each continent: seeds whose land discs overlap belong to one continent, and its id is the smallest cell key
 * among them, so every member finds the same id.
 */
public final class ContinentIds {

    /** Seeds searched before giving up; the largest continents measured hold about 1300. */
    private static final int MAX_SEEDS = 20000;
    private static final int LIMIT = 200000;
    private static final Map<Long, Long> IDS = new ConcurrentHashMap<>();
    private static volatile long cachedSeed;
    private static volatile boolean seeded;

    private ContinentIds() {}

    public static long of(long seed, Continent.Seed s) {
        if (!seeded || seed != cachedSeed) {
            IDS.clear();
            cachedSeed = seed;
            seeded = true;
        }
        Long id = IDS.get(key(s));
        if (id != null) return id;
        List<Long> members = new ArrayList<>();
        long found = fill(seed, s, members);
        if (IDS.size() + members.size() > LIMIT) IDS.clear();
        for (long m : members) IDS.put(m, found);
        return found;
    }

    /** The id without the cache. */
    static long compute(long seed, Continent.Seed s) {
        return fill(seed, s, new ArrayList<>());
    }

    private static long fill(long seed, Continent.Seed start, List<Long> members) {
        // Every cell looked at, empty ones included, so no cell's noise is sampled twice.
        Map<Long, Continent.Seed> cells = new HashMap<>();
        Set<Long> joined = new HashSet<>();
        ArrayDeque<Continent.Seed> queue = new ArrayDeque<>();
        long startKey = key(start);
        members.add(startKey);
        joined.add(startKey);
        queue.add(start);
        long smallest = startKey;
        int reach = Continent.neighbourCells();
        while (!queue.isEmpty() && members.size() < MAX_SEEDS) {
            Continent.Seed s = queue.poll();
            int cx = Continent.cellOf(s.x), cz = Continent.cellOf(s.z);
            for (int i = cx - reach; i <= cx + reach; i++) {
                for (int j = cz - reach; j <= cz + reach; j++) {
                    long k = key(i, j);
                    if (joined.contains(k)) continue;
                    Continent.Seed o;
                    if (cells.containsKey(k)) o = cells.get(k);
                    else {
                        o = Continent.seedAt(seed, i, j);
                        cells.put(k, o);
                    }
                    if (o == null || Math.hypot(o.x - s.x, o.z - s.z) > Continent.rimRadius(s) + Continent.rimRadius(o))
                        continue;
                    joined.add(k);
                    members.add(k);
                    queue.add(o);
                    smallest = Math.min(smallest, k);
                }
            }
        }
        return smallest;
    }

    private static long key(Continent.Seed s) {
        return key(Continent.cellOf(s.x), Continent.cellOf(s.z));
    }

    private static long key(int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
    }
}
