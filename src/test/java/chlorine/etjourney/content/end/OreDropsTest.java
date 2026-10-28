package chlorine.etjourney.content.end;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

/** Ore drops follow the vanilla diamond ore: Fortune multiplies the count by 1 to level + 1, weighted towards 1. */
class OreDropsTest {

    private static double mean(int min, int max, int fortune) {
        Random random = new Random(7);
        long total = 0;
        for (int i = 0; i < 20000; i++) total += OreDrops.count(min, max, fortune, random);
        return total / 20000.0;
    }

    @Test
    void withoutFortuneTheCountStaysInItsRange() {
        Random random = new Random(1);
        boolean one = false, two = false;
        for (int i = 0; i < 2000; i++) {
            int n = OreDrops.count(1, 2, 0, random);
            assertTrue(n == 1 || n == 2, "count " + n);
            one |= n == 1;
            two |= n == 2;
        }
        assertTrue(one && two);
        for (int i = 0; i < 100; i++) assertEquals(1, OreDrops.count(1, 1, 0, random));
    }

    @Test
    void fortuneMultipliesUpToLevelPlusOne() {
        Random random = new Random(2);
        int most = 0;
        for (int i = 0; i < 5000; i++) {
            int n = OreDrops.count(1, 1, 3, random);
            assertTrue(n >= 1 && n <= 4, "count " + n);
            most = Math.max(most, n);
        }
        assertEquals(4, most);
    }

    @Test
    void fortuneMatchesTheDiamondOreAverage() {
        // Diamond ore: Fortune I gives 1.33, II 1.75, III 2.2 on average.
        assertEquals(1.33, mean(1, 1, 1), 0.03);
        assertEquals(1.75, mean(1, 1, 2), 0.03);
        assertEquals(2.2, mean(1, 1, 3), 0.04);
    }

    @Test
    void badInputsDropOneAndNeverThrow() {
        Random random = new Random(3);
        assertEquals(1, OreDrops.count(1, 1, -5, random));
        assertEquals(1, OreDrops.count(0, 0, 0, random));
        assertEquals(2, OreDrops.count(2, 1, 0, random));
    }
}
