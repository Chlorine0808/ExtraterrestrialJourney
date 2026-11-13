package chlorine.etjourney.content.worm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.content.worm.WormPhases.Medium;
import chlorine.etjourney.content.worm.WormPhases.Phase;

/** Swim, warn, leap, swim again: when a worm moves on, and for how long it warns. */
class WormPhasesTest {

    private static WormPhases ground() {
        return new WormPhases(Medium.GROUND, 30, 20, 200, 60, 12);
    }

    private static WormPhases air() {
        return new WormPhases(Medium.AIR, 10, 20, 200, 60, 12);
    }

    /** Repeats one tick with the same inputs. */
    private static void run(WormPhases p, int ticks, boolean target, double dist, boolean buried, boolean falling) {
        for (int i = 0; i < ticks; i++) p.tick(target, dist, buried, falling);
    }

    @Test
    void aBuriedWormWarnsOnceItsTargetIsInRange() {
        WormPhases p = ground();
        p.tick(true, 20, true, false);
        assertEquals(Phase.ROAM, p.phase());
        p.tick(true, 12, true, false);
        assertEquals(Phase.WARN, p.phase());
    }

    @Test
    void aGroundWormOutOfTheGroundDoesNotWarn() {
        WormPhases p = ground();
        p.tick(true, 5, false, false);
        assertEquals(Phase.ROAM, p.phase());
    }

    @Test
    void theWarningLastsItsTicksAndThenTheWormLeaps() {
        WormPhases p = ground();
        p.tick(true, 5, true, false);
        run(p, 29, true, 5, true, false);
        assertEquals(Phase.WARN, p.phase());
        assertTrue(p.tick(true, 5, true, false), "the leap starts on the 30th tick");
        assertEquals(Phase.LEAP, p.phase());
    }

    @Test
    void losingTheTargetMidWarningCallsTheStrikeOffWithoutACooldown() {
        WormPhases p = ground();
        p.tick(true, 5, true, false);
        run(p, 10, true, 5, true, false);
        p.tick(false, 0, true, false);
        assertEquals(Phase.ROAM, p.phase());
        p.tick(true, 5, true, false);
        assertEquals(Phase.WARN, p.phase());
    }

    @Test
    void aGroundLeapEndsWhenTheHeadSinksBackIntoTheGround() {
        WormPhases p = ground();
        p.tick(true, 5, true, false);
        run(p, 30, true, 5, true, false);
        // Still rising out of the ground: buried but not falling.
        p.tick(true, 5, true, false);
        assertEquals(Phase.LEAP, p.phase());
        run(p, 15, true, 5, false, true);
        assertEquals(Phase.LEAP, p.phase());
        p.tick(true, 5, true, true);
        assertEquals(Phase.ROAM, p.phase());
    }

    @Test
    void afterALeapTheWormWaitsOutItsCooldownBeforeWarningAgain() {
        WormPhases p = ground();
        p.tick(true, 5, true, false);
        run(p, 30, true, 5, true, false);
        p.tick(true, 5, true, true);
        assertEquals(Phase.ROAM, p.phase());
        run(p, 59, true, 5, true, false);
        assertEquals(Phase.ROAM, p.phase());
        p.tick(true, 5, true, false);
        assertEquals(Phase.WARN, p.phase());
    }

    @Test
    void aLeapThatNeverFindsGroundIsCutOff() {
        WormPhases p = ground();
        p.tick(true, 5, true, false);
        run(p, 30, true, 5, true, false);
        run(p, 199, true, 5, false, true);
        assertEquals(Phase.LEAP, p.phase());
        p.tick(true, 5, false, true);
        assertEquals(Phase.ROAM, p.phase());
    }

    @Test
    void anAirWormWarnsAnywhereAndDashesForItsLeapTicks() {
        WormPhases p = air();
        p.tick(true, 5, false, false);
        assertEquals(Phase.WARN, p.phase());
        run(p, 10, true, 5, false, false);
        assertEquals(Phase.LEAP, p.phase());
        run(p, 19, true, 5, false, false);
        assertEquals(Phase.LEAP, p.phase());
        p.tick(true, 5, false, false);
        assertEquals(Phase.ROAM, p.phase());
    }

    @Test
    void noWarningTicksMeansTheLeapStartsAtOnce() {
        WormPhases p = new WormPhases(Medium.AIR, 0, 20, 200, 60, 12);
        assertTrue(p.tick(true, 5, false, false));
        assertEquals(Phase.LEAP, p.phase());
    }

    @Test
    void theDustThickensAsTheWarningRunsOut() {
        assertEquals(1, WormPhases.warnDust(0, 30));
        assertEquals(4, WormPhases.warnDust(29, 30));
        assertTrue(WormPhases.warnDust(10, 30) <= WormPhases.warnDust(20, 30));
        assertFalse(WormPhases.warnDust(15, 30) > 4);
    }
}
