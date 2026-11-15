package chlorine.etjourney.content.rank.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The pull points at the mob, grows with the distance, is capped, and works only between 4 and 16 blocks. */
class PullAbilityTest {

    @Test
    void pointsFromThePlayerToTheMob() {
        double[] v = PullAbility.pull(-8, 6, 10);
        assertNotNull(v);
        assertTrue(v[0] < 0 && v[2] > 0);
        assertEquals(0.8 / 0.6, -v[0] / v[2], 1e-9);
    }

    @Test
    void growsWithTheDistance() {
        double near = Math.hypot(PullAbility.pull(5, 0, 5)[0], PullAbility.pull(5, 0, 5)[2]);
        double far = Math.hypot(PullAbility.pull(9, 0, 9)[0], PullAbility.pull(9, 0, 9)[2]);
        assertTrue(far > near);
    }

    @Test
    void isCappedAndLifts() {
        double[] v = PullAbility.pull(16, 0, 16);
        assertEquals(PullAbility.MAX_SPEED, v[0], 1e-9);
        assertEquals(PullAbility.LIFT, v[1], 1e-9);
    }

    @Test
    void doesNothingOutsideTheWindow() {
        assertNull(PullAbility.pull(3, 0, 3.9));
        assertNull(PullAbility.pull(17, 0, 16.1));
        assertNotNull(PullAbility.pull(4, 0, 4));
        assertNotNull(PullAbility.pull(16, 0, 16));
    }
}
