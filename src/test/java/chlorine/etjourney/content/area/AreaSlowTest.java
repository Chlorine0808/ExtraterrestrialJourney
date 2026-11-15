package chlorine.etjourney.content.area;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Walking speed alone let creatures hop across a slowing area at full air speed, so the jump is held back too:
 * its height shrinks by the same factor as the walk, which needs the take-off speed's square root.
 */
class AreaSlowTest {

    private static final double EPSILON = 1e-9;

    @Test
    void theJumpHeightShrinksByTheSpeedFactor() {
        double left = AreaSlow.jumpLeft(0.3);
        assertEquals(0.3, left * left, EPSILON);
    }

    @Test
    void aFreeCreatureJumpsAsUsual() {
        assertEquals(1, AreaSlow.jumpLeft(1), EPSILON);
    }

    @Test
    void aStoppedCreatureCannotJump() {
        assertEquals(0, AreaSlow.jumpLeft(0), EPSILON);
    }
}
