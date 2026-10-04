package chlorine.etjourney.world.end.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class CellCacheTest {

    @Test
    void computesOncePerCellAndSeedAndCachesNulls() {
        AtomicInteger calls = new AtomicInteger();
        CellCache<String> cache = new CellCache<>(100);
        CellCache.CellFunction<String> f = (seed, cx, cz) -> {
            calls.incrementAndGet();
            return cx == 0 ? null : seed + ":" + cx + "," + cz;
        };
        assertEquals("1:2,3", cache.get(1L, 2, 3, f));
        assertEquals("1:2,3", cache.get(1L, 2, 3, f));
        assertNull(cache.get(1L, 0, 0, f));
        assertNull(cache.get(1L, 0, 0, f));
        assertEquals(2, calls.get());
        assertEquals("2:2,3", cache.get(2L, 2, 3, f));
        assertEquals(3, calls.get());
    }
}
