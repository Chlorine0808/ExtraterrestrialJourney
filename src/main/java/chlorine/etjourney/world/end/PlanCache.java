package chlorine.etjourney.world.end;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** The last few chunk plans: a chunk's three passes arrive close together and share one plan. */
public final class PlanCache {

    private static final int SIZE = 64;

    private final Map<Long, ChunkPlan> plans = new LinkedHashMap<Long, ChunkPlan>(SIZE, 0.75f, true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, ChunkPlan> eldest) {
            return size() > SIZE;
        }
    };
    private long seed;

    public synchronized ChunkPlan get(long worldSeed, int chunkX, int chunkZ, Supplier<ChunkPlan> build) {
        if (worldSeed != seed) {
            plans.clear();
            seed = worldSeed;
        }
        long key = ((long) chunkX << 32) ^ (chunkZ & 0xFFFFFFFFL);
        ChunkPlan plan = plans.get(key);
        if (plan == null) {
            plan = build.get();
            plans.put(key, plan);
        }
        return plan;
    }
}
