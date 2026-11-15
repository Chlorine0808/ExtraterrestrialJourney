package chlorine.etjourney.content.rank;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

/** The render scale is the split child's shrink times the factor of every ability. */
class RankSyncTest {

    @Test
    void anOrdinaryMobIsDrawnAtFullSize() {
        assertEquals(1f, RankSync.combine(false, Collections.emptyList()), 1e-6);
    }

    @Test
    void aSplitChildIsShrunk() {
        assertEquals(RankSync.SPLIT_CHILD_SCALE, RankSync.combine(true, Collections.emptyList()), 1e-6);
    }

    @Test
    void theFactorsMultiply() {
        assertEquals(1.5f * 0.5f * 0.75f, RankSync.combine(true, Arrays.asList(1.5f, 1f, 0.5f)), 1e-6);
    }
}
