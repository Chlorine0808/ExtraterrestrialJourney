package chlorine.etjourney.world.end.shoot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * The screenshot world is created when missing, reused with a freshly generated End when its seed matches, and
 * rebuilt when the seed differs, so every run shoots the End of the wanted seed with the current generator.
 */
class WorldPlanTest {

    @Test
    void noSaveIsCreated() {
        assertEquals(WorldPlan.Action.CREATE, WorldPlan.decide(null, 1L));
    }

    @Test
    void sameSeedIsLoadedWithAFreshEnd() {
        assertEquals(WorldPlan.Action.LOAD_WITH_FRESH_END, WorldPlan.decide(1L, 1L));
    }

    @Test
    void otherSeedIsRecreated() {
        assertEquals(WorldPlan.Action.RECREATE, WorldPlan.decide(2L, 1L));
    }
}
