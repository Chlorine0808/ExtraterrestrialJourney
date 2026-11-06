package chlorine.etjourney.content.craft;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Each condition reads only its own part of the place. */
class ConditionTest {

    private static ConditionContext at(long worldTime) {
        return new ConditionContext("Grove", 1, true, worldTime);
    }

    @Test
    void nightRunsFromDuskToDawnOnEveryDay() {
        NightCondition night = new NightCondition();
        assertFalse(night.isMet(at(6000L)));
        assertFalse(night.isMet(at(12999L)));
        assertTrue(night.isMet(at(13000L)));
        assertTrue(night.isMet(at(22999L)));
        assertFalse(night.isMet(at(23000L)));
        assertTrue(night.isMet(at(24000L * 5 + 18000L)));
    }

    @Test
    void dimensionMatchesAnyListedId() {
        DimensionCondition endOrNether = new DimensionCondition(1, -1);
        assertTrue(endOrNether.isMet(new ConditionContext("Grove", 1, true, 0L)));
        assertTrue(endOrNether.isMet(new ConditionContext("Hell", -1, false, 0L)));
        assertFalse(endOrNether.isMet(new ConditionContext("Plains", 0, true, 0L)));
    }

    @Test
    void biomeAndSkyReadOnlyTheirOwnPart() {
        assertTrue(new BiomeCondition("Grove", "Highland").isMet(new ConditionContext("Highland", 0, false, 0L)));
        assertFalse(new BiomeCondition("Grove").isMet(new ConditionContext("Highland", 1, true, 0L)));
        assertTrue(new SkyCondition().isMet(new ConditionContext("Plains", 0, true, 0L)));
        assertFalse(new SkyCondition().isMet(new ConditionContext("Grove", 1, false, 18000L)));
    }
}
