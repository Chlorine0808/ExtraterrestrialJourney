package chlorine.etjourney.content.rank;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.content.rank.MinionWatch.Parent;

/** A minion vanishes at once when its parent dies, and after a grace when the parent cannot be found. */
class MinionWatchTest {

    @Test
    void aDeadParentTakesTheMinionAtOnce() {
        assertEquals(MinionWatch.VANISH, MinionWatch.step(0, Parent.DEAD));
    }

    @Test
    void aMissingParentGetsAGrace() {
        int misses = 0;
        for (int i = 1; i < MinionWatch.GRACE; i++) {
            misses = MinionWatch.step(misses, Parent.MISSING);
            assertEquals(i, misses);
        }
        assertEquals(MinionWatch.VANISH, MinionWatch.step(misses, Parent.MISSING));
    }

    @Test
    void findingTheParentAgainResetsTheGrace() {
        assertEquals(0, MinionWatch.step(MinionWatch.GRACE - 1, Parent.ALIVE));
    }

    @Test
    void summonsFillUpToTheCap() {
        assertEquals(2, MinionWatch.toSummon(0, 4, 2));
        assertEquals(1, MinionWatch.toSummon(3, 4, 2));
        assertEquals(0, MinionWatch.toSummon(4, 4, 2));
        assertEquals(0, MinionWatch.toSummon(6, 4, 2));
    }
}
