package chlorine.etjourney.content.area;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.content.projectile.Faction;

/** Whom an area works on: the shot factions, widened to fellow mobs only when it takes allies along. */
class AreaTargetsTest {

    @Test
    void anEnemyAreaSparesOtherMobsUnlessItTakesAlliesAlong() {
        assertTrue(AreaTargets.affects(Faction.ENEMY, false, true, false));
        assertFalse(AreaTargets.affects(Faction.ENEMY, false, false, false));
        assertTrue(AreaTargets.affects(Faction.ENEMY, true, false, false));
        assertTrue(AreaTargets.affects(Faction.ENEMY, true, true, false));
    }

    @Test
    void aPlayersAreaWorksOnEveryoneButThem() {
        assertTrue(AreaTargets.affects(Faction.PLAYER, false, false, false));
        assertTrue(AreaTargets.affects(Faction.PLAYER, false, true, false));
        assertFalse(AreaTargets.affects(Faction.PLAYER, false, true, true));
    }

    @Test
    void whoeverLeftItIsNeverCaughtInIt() {
        assertFalse(AreaTargets.affects(Faction.ENEMY, true, false, true));
        assertFalse(AreaTargets.affects(Faction.PLAYER, true, true, true));
    }
}
