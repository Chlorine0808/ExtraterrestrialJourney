package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

/** Far from every island the density keeps falling, so chunks that gather different islands still agree. */
class ZoneIslandDensityTest {

    @Test
    void aDistantIslandDoesNotLiftTheDensity() {
        ZoneIslands.Island island = new ZoneIslands.Island(0, 120, 0, 60);
        double d = ZoneIslands.density(Collections.singletonList(island), 1000, 120, 0, 1);
        assertTrue(d < -100, "density 1000 blocks away: " + d);
    }

    @Test
    void noIslandsLeaveNothingSolid() {
        double d = ZoneIslands.density(Collections.<ZoneIslands.Island>emptyList(), 0, 120, 0, 1);
        assertTrue(d < -100, "density with no islands: " + d);
    }
}
