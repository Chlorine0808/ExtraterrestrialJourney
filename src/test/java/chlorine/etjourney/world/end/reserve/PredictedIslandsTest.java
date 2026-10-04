package chlorine.etjourney.world.end.reserve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Random;

import org.junit.jupiter.api.Test;

class PredictedIslandsTest {

    @Test
    void chunkSeedFollowsFmlsShiftByThree() {
        long seed = 7037748451318983361L;
        Random r = new Random(seed);
        long xs = r.nextLong() >> 3, zs = r.nextLong() >> 3;
        assertEquals((xs * 70 + zs * -12) ^ seed, PredictedIslands.chunkSeed(seed, 70, -12));
    }

    @Test
    void noStartsInsideTheMinimumDistance() {
        PredictedIslands.Rule rule = new PredictedIslands.Rule("test", 2, 0, 1000);
        for (int cx = -50; cx <= 50; cx++) assertFalse(PredictedIslands.startsIn(rule, 5L, cx, 3));
    }
}
