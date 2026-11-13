package chlorine.etjourney.content.end.spawncheck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The spawn check's verdicts: the new enemy where it belongs, and endermen kept to at least 70% of the baseline. */
class SpawnTallyTest {

    @Test
    void endermenAtSeventyPercentOfTheBaselinePass() {
        assertTrue(SpawnTally.endermenKept(7, 10));
        assertFalse(SpawnTally.endermenKept(6, 10));
    }

    @Test
    void anEmptyBaselineCannotBeJudged() {
        assertFalse(SpawnTally.endermenKept(0, 0));
    }

    @Test
    void shareIsTheEnemysPartOfAllCounted() {
        assertEquals(0.25, SpawnTally.share(1, 3), 1e-9);
        assertEquals(0, SpawnTally.share(0, 0), 1e-9);
    }

    @Test
    void aRankedCountNearTheChanceFits() {
        assertTrue(SpawnTally.rankShareFits(10, 250, 0.04));
        assertTrue(SpawnTally.rankShareFits(20, 250, 0.04));
        assertFalse(SpawnTally.rankShareFits(40, 250, 0.04));
    }

    @Test
    void noMobsCannotBeJudged() {
        assertFalse(SpawnTally.rankShareFits(0, 0, 0.04));
    }
}
