package chlorine.etjourney.core.damage.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DamageMathTest {

    private static final Element FIRE = ElementRegistry.register("test_math_fire");
    private static final Element NEUTRAL = ElementRegistry.NEUTRAL;

    /** The Solar sword of the spec: neutral 6 + fire 4. */
    private static ElementalDamage sword() {
        return ElementalDamage.builder(DamageKind.PHYSICAL)
            .add(NEUTRAL, 6)
            .add(FIRE, 4)
            .build();
    }

    @Test
    void takesTheRawSumWithoutResistance() {
        assertEquals(10f, DamageMath.resolve(sword(), Resistance.NONE), 1e-6);
    }

    @Test
    void multipliesBeforeSubtracting() {
        Resistance r = Resistance.builder()
            .multiply(FIRE, 0.5f)
            .subtract(FIRE, 1)
            .build();
        assertEquals(7f, DamageMath.resolve(sword(), r), 1e-6);
    }

    @Test
    void neverGoesBelowZeroPerElement() {
        Resistance r = Resistance.builder()
            .multiply(FIRE, 0.1f)
            .subtract(FIRE, 5)
            .build();
        assertEquals(6f, DamageMath.resolve(sword(), r), 1e-6);
    }

    @Test
    void weaknessRaisesTheDamage() {
        Resistance r = Resistance.builder()
            .multiply(FIRE, 1.5f)
            .build();
        assertEquals(12f, DamageMath.resolve(sword(), r), 1e-6);
    }

    @Test
    void appliesTheKindFactor() {
        ElementalDamage d = ElementalDamage.builder(DamageKind.MAGIC)
            .add(NEUTRAL, 10)
            .build();
        Resistance r = Resistance.builder()
            .multiplyKind(DamageKind.MAGIC, 0.05f)
            .build();
        assertEquals(0.5f, DamageMath.resolve(d, r), 1e-6);
        assertEquals(10f, DamageMath.resolve(sword(), r), 1e-6);
    }

    @Test
    void multipliesFactorsAndAddsFlatsFromSeveralSources() {
        Resistance r = Resistance.builder()
            .multiply(FIRE, 0.5f)
            .multiply(FIRE, 0.5f)
            .build();
        assertEquals(7f, DamageMath.resolve(sword(), r), 1e-6);
        Resistance flats = Resistance.builder()
            .subtract(FIRE, 1)
            .subtract(FIRE, 1)
            .build();
        assertEquals(8f, DamageMath.resolve(sword(), flats), 1e-6);
    }

    @Test
    void treatsANegativeFactorAsZero() {
        Resistance r = Resistance.builder()
            .multiply(FIRE, -1)
            .build();
        assertEquals(6f, DamageMath.resolve(sword(), r), 1e-6);
    }

    @Test
    void resolvesAnEmptyDamageToZero() {
        ElementalDamage empty = ElementalDamage.builder(DamageKind.PHYSICAL)
            .build();
        assertEquals(0f, DamageMath.resolve(empty, Resistance.NONE), 1e-6);
    }

    @Test
    void scalesTheIncomingAmountByTheResolvedShare() {
        Resistance r = Resistance.builder()
            .multiply(FIRE, 0.5f)
            .subtract(FIRE, 1)
            .build();
        // Inside the hurt cooldown vanilla passes only the excess over the last hit.
        assertEquals(2.8f, DamageMath.scale(4, sword(), r), 1e-6);
    }

    @Test
    void leavesTheIncomingAmountWhenTheRawSumIsZero() {
        ElementalDamage empty = ElementalDamage.builder(DamageKind.PHYSICAL)
            .add(FIRE, 0)
            .build();
        assertEquals(3f, DamageMath.scale(3, empty, Resistance.NONE), 1e-6);
    }

    @Test
    void shortfallIsWhatTheHitFailedToTake() {
        assertEquals(5f, DamageMath.shortfall(20, 20, 5), 1e-6);
        assertEquals(2f, DamageMath.shortfall(20, 17, 5), 1e-6);
        assertEquals(0f, DamageMath.shortfall(20, 10, 5), 1e-6);
    }
}
