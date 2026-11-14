package chlorine.etjourney.content.projectile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Enemy shots hit only players; a player's shots hit anything alive but the player who fired. */
class FactionTest {

    @Test
    void aShooterlessShotIsAnEnemyShot() {
        assertEquals(Faction.ENEMY, Faction.of(false));
        assertEquals(Faction.PLAYER, Faction.of(true));
    }

    @Test
    void enemyShotsPassThroughOtherMobs() {
        assertTrue(Faction.ENEMY.hits(true, false));
        assertFalse(Faction.ENEMY.hits(false, false));
    }

    @Test
    void playerShotsHitMobsAndOtherPlayersButNotTheShooter() {
        assertTrue(Faction.PLAYER.hits(false, false));
        assertTrue(Faction.PLAYER.hits(true, false));
        assertFalse(Faction.PLAYER.hits(true, true));
    }
}
