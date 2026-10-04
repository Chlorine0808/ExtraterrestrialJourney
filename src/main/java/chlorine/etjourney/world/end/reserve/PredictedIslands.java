package chlorine.etjourney.world.end.reserve;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Predicts where world generators that gate on their first roll (such as NovaCraft's End islands) start an island.
 * FML reseeds the shared Random with the chunk seed before every generator, so that roll is reproducible. Used only
 * to find those islands; our terrain does not make room for them (islands built into a cleared area came out with
 * only their underside).
 */
public final class PredictedIslands {

    /** One generator: it starts an island in a chunk when its first nextInt(rate) equals hit. */
    public static final class Rule {

        public final String label;
        final int rate, hit, minBlock;
        final Map<Long, Boolean> cache = new ConcurrentHashMap<>();
        volatile long cacheSeed;

        public Rule(String label, int rate, int hit, int minBlock) {
            this.label = label;
            this.rate = rate;
            this.hit = hit;
            this.minBlock = minBlock;
        }
    }

    private static final List<Rule> RULES = new CopyOnWriteArrayList<>();
    private static long cachedWorldSeed;
    private static long xSeed, zSeed;
    private static boolean cached;

    private PredictedIslands() {}

    public static void register(Rule rule) {
        if (rule.rate > 0) RULES.add(rule);
    }

    /** Island starts of the labelled rule within chunkRadius, as areas centred one chunk in from the start corner. */
    public static List<Area> near(long worldSeed, String label, int chunkX, int chunkZ, int chunkRadius) {
        List<Area> out = new ArrayList<>();
        for (Rule rule : RULES) {
            if (!rule.label.equals(label)) continue;
            for (int cx = chunkX - chunkRadius; cx <= chunkX + chunkRadius; cx++) {
                for (int cz = chunkZ - chunkRadius; cz <= chunkZ + chunkRadius; cz++) {
                    if (startsIn(rule, worldSeed, cx, cz)) out.add(new Area(rule.label, cx * 16 + 16, cz * 16 + 16, 0));
                }
            }
        }
        return out;
    }

    static boolean startsIn(Rule rule, long worldSeed, int chunkX, int chunkZ) {
        if (Math.abs(chunkX * 16) < rule.minBlock && Math.abs(chunkZ * 16) < rule.minBlock) return false;
        if (rule.cacheSeed != worldSeed) {
            rule.cache.clear();
            rule.cacheSeed = worldSeed;
        }
        long key = ((long) chunkX << 32) ^ (chunkZ & 0xFFFFFFFFL);
        Boolean hit = rule.cache.get(key);
        if (hit == null) {
            hit = new Random(chunkSeed(worldSeed, chunkX, chunkZ)).nextInt(rule.rate) == rule.hit;
            if (rule.cache.size() > 65536) rule.cache.clear();
            rule.cache.put(key, hit);
        }
        return hit;
    }

    /** Same arithmetic as GameRegistry.generateWorld, including its "nextLong() >> 2 + 1L" (a shift by 3). */
    static synchronized long chunkSeed(long worldSeed, int chunkX, int chunkZ) {
        if (!cached || cachedWorldSeed != worldSeed) {
            Random r = new Random(worldSeed);
            xSeed = r.nextLong() >> 2 + 1L;
            zSeed = r.nextLong() >> 2 + 1L;
            cachedWorldSeed = worldSeed;
            cached = true;
        }
        return (xSeed * chunkX + zSeed * chunkZ) ^ worldSeed;
    }
}
