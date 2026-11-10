package chlorine.etjourney.content.end;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import org.junit.jupiter.api.Test;

/** Each looting level raises the most a drop can roll by one, and the least stays zero. */
class SporeZombieDropsTest {

    private static int[] range(int max, int looting) {
        Random random = new Random(1);
        int least = Integer.MAX_VALUE, most = Integer.MIN_VALUE;
        for (int i = 0; i < 2000; i++) {
            int n = SporeZombieDrops.count(max, looting, random);
            least = Math.min(least, n);
            most = Math.max(most, n);
        }
        return new int[] { least, most };
    }

    @Test
    void withoutLootingTheCountStaysWithinZeroToMax() {
        assertEquals(0, range(2, 0)[0]);
        assertEquals(2, range(2, 0)[1]);
    }

    @Test
    void eachLootingLevelAddsOneToTheMost() {
        assertEquals(3, range(2, 1)[1]);
        assertEquals(4, range(1, 3)[1]);
        assertEquals(0, range(1, 3)[0]);
    }
}
