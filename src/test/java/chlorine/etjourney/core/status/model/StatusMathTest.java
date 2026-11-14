package chlorine.etjourney.core.status.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class StatusMathTest {

    private static final String POISON = "potion.poison";
    private static final String SPEED = "potion.moveSpeed";

    @Test
    void nothingKeepsTheEffect() {
        StatusMath.Result r = StatusMath.resolve(200, 1, POISON, true, StatusBoosts.NONE, StatusResistance.NONE);
        assertEquals(200, r.duration);
        assertEquals(1, r.amplifier);
    }

    @Test
    void giverBonusesAddAndTakerFactorsMultiply() {
        StatusBoosts boosts = StatusBoosts.builder()
            .addDuration(POISON, 0.3f)
            .addDuration(StatusMath.ANY, 0.2f)
            .build();
        StatusResistance resistance = StatusResistance.builder()
            .multiplyDuration(POISON, 0.5f)
            .multiplyDuration(StatusMath.ANY, 0.5f)
            .build();
        // 200 x (1 + 0.3 + 0.2) x 0.5 x 0.5
        assertEquals(75, StatusMath.resolve(200, 0, POISON, true, boosts, resistance).duration);
    }

    @Test
    void levelsAddAndCutButNeverBelowOne() {
        StatusBoosts boosts = StatusBoosts.builder()
            .addLevel(POISON, 1)
            .build();
        StatusResistance resistance = StatusResistance.builder()
            .reduceLevel(StatusMath.ANY, 1)
            .reduceLevel(POISON, 1)
            .build();
        assertEquals(1, StatusMath.resolve(20, 2, POISON, true, boosts, resistance).amplifier);
        assertEquals(0, StatusMath.resolve(20, 0, POISON, true, StatusBoosts.NONE, resistance).amplifier);
    }

    @Test
    void anyTouchesOnlyHarmfulEffects() {
        StatusResistance resistance = StatusResistance.builder()
            .multiplyDuration(StatusMath.ANY, 0.5f)
            .reduceLevel(StatusMath.ANY, 1)
            .build();
        StatusMath.Result r = StatusMath.resolve(200, 1, SPEED, false, StatusBoosts.NONE, resistance);
        assertEquals(200, r.duration);
        assertEquals(1, r.amplifier);
    }

    @Test
    void aZeroFactorBlocksTheEffect() {
        StatusResistance immune = StatusResistance.builder()
            .multiplyDuration(POISON, 0)
            .build();
        assertNull(StatusMath.resolve(200, 0, POISON, true, StatusBoosts.NONE, immune));
    }

    @Test
    void negativeBonusesStopAtZero() {
        StatusBoosts weak = StatusBoosts.builder()
            .addDuration(POISON, -2)
            .build();
        assertNull(StatusMath.resolve(200, 0, POISON, true, weak, StatusResistance.NONE));
    }

    @Test
    void aShortEffectRoundsToTheNearestTick() {
        StatusResistance resistance = StatusResistance.builder()
            .multiplyDuration(POISON, 0.5f)
            .build();
        assertEquals(2, StatusMath.resolve(3, 0, POISON, true, StatusBoosts.NONE, resistance).duration);
    }
}
