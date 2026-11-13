package chlorine.etjourney.content.rank;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

/** A drop is multiplied and split into stacks no larger than the item allows. */
class RankLootTest {

    @Test
    void multipliesWithinOneStack() {
        assertEquals(Arrays.asList(6), RankLoot.stacks(3, 2, 64));
    }

    @Test
    void splitsOverTheMaximum() {
        assertEquals(Arrays.asList(64, 56), RankLoot.stacks(40, 3, 64));
        assertEquals(Arrays.asList(1, 1, 1, 1), RankLoot.stacks(1, 4, 1));
    }

    @Test
    void anEmptyDropStaysEmpty() {
        assertEquals(Arrays.asList(), RankLoot.stacks(0, 3, 64));
    }
}
