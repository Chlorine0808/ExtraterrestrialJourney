package chlorine.etjourney.world.end;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.region.RegionPicker;
import chlorine.etjourney.world.end.region.Styles;

/** The plan cache shares a chunk's plan, drops the least recently used past its size and forgets on a new seed. */
class PlanCacheTest {

    private static final TerrainSampler SAMPLER = new TerrainSampler(1L, new RegionPicker(Styles.all()));

    private static ChunkPlan get(PlanCache c, long seed, int cx, int cz, AtomicInteger builds) {
        return c.get(seed, cx, cz, () -> {
            builds.incrementAndGet();
            return new ChunkPlan(SAMPLER, cx, cz, (x, z) -> Collections.emptyList());
        });
    }

    @Test
    void repeatedLookupBuildsOnce() {
        PlanCache c = new PlanCache();
        AtomicInteger builds = new AtomicInteger();
        ChunkPlan a = get(c, 1L, 2, 3, builds);
        assertSame(a, get(c, 1L, 2, 3, builds));
        assertEquals(1, builds.get());
        assertNotSame(a, get(c, 1L, 3, 2, builds));
        assertEquals(2, builds.get());
    }

    @Test
    void leastRecentlyUsedPlanIsEvicted() {
        PlanCache c = new PlanCache();
        AtomicInteger builds = new AtomicInteger();
        for (int i = 0; i < 64; i++) get(c, 1L, i, 0, builds);
        // Touch chunk 0 so chunk 1 becomes the eldest, then overflow by one.
        get(c, 1L, 0, 0, builds);
        get(c, 1L, 64, 0, builds);
        assertEquals(65, builds.get());
        get(c, 1L, 0, 0, builds);
        assertEquals(65, builds.get());
        get(c, 1L, 1, 0, builds);
        assertEquals(66, builds.get());
    }

    @Test
    void newSeedDropsEveryPlan() {
        PlanCache c = new PlanCache();
        AtomicInteger builds = new AtomicInteger();
        ChunkPlan a = get(c, 1L, 5, 5, builds);
        assertNotSame(a, get(c, 2L, 5, 5, builds));
        assertEquals(2, builds.get());
    }

    @Test
    void negativeCoordinatesDoNotCollide() {
        PlanCache c = new PlanCache();
        AtomicInteger builds = new AtomicInteger();
        get(c, 1L, -1, 0, builds);
        get(c, 1L, 0, -1, builds);
        get(c, 1L, -1, -1, builds);
        assertEquals(3, builds.get());
    }
}
