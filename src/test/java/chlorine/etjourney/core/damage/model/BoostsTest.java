package chlorine.etjourney.core.damage.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

class BoostsTest {

    private static final Element FIRE = ElementRegistry.register("test_boost_fire");
    private static final Element NEUTRAL = ElementRegistry.NEUTRAL;
    private static final AttackProfile SWORD = new AttackProfile(
        DamageKind.PHYSICAL,
        Reach.MELEE,
        WeaponClass.SWORD,
        false);
    private static final AttackProfile SPELL = new AttackProfile(
        DamageKind.MAGIC,
        Reach.RANGED,
        WeaponClass.SPELL,
        false);

    @Test
    void nothingLeavesTheFactorAtOne() {
        assertEquals(1f, Boosts.NONE.factor(FIRE, SWORD), 1e-6);
    }

    @Test
    void matchingBonusesAdd() {
        Boosts b = Boosts.builder()
            .add(BoostFilter.ALL, 0.2f)
            .add(BoostFilter.element(FIRE), 0.3f)
            .build();
        assertEquals(1.5f, b.factor(FIRE, SWORD), 1e-6);
        assertEquals(1.2f, b.factor(NEUTRAL, SWORD), 1e-6);
    }

    @Test
    void aNarrowFilterNeedsEveryAxis() {
        Boosts b = Boosts.builder()
            .add(
                BoostFilter.element(FIRE)
                    .kind(DamageKind.PHYSICAL)
                    .reach(Reach.MELEE),
                0.5f)
            .build();
        assertEquals(1.5f, b.factor(FIRE, SWORD), 1e-6);
        assertEquals(1f, b.factor(FIRE, SPELL), 1e-6);
        assertEquals(1f, b.factor(NEUTRAL, SWORD), 1e-6);
    }

    @Test
    void weaponAndSummonFiltersMatch() {
        AttackProfile minion = new AttackProfile(DamageKind.PHYSICAL, Reach.MELEE, WeaponClass.UNARMED, true);
        Boosts b = Boosts.builder()
            .add(BoostFilter.ALL.weapon(WeaponClass.SWORD), 0.1f)
            .add(BoostFilter.ALL.summoned(true), 0.4f)
            .build();
        assertEquals(1.1f, b.factor(NEUTRAL, SWORD), 1e-6);
        assertEquals(1.4f, b.factor(NEUTRAL, minion), 1e-6);
    }

    @Test
    void theFactorStopsAtZero() {
        Boosts b = Boosts.builder()
            .add(BoostFilter.ALL, -2f)
            .build();
        assertEquals(0f, b.factor(FIRE, SWORD), 1e-6);
    }

    @Test
    void buildersMerge() {
        Boosts.Builder out = Boosts.builder()
            .add(BoostFilter.ALL, 0.1f);
        out.addAll(
            Boosts.builder()
                .add(BoostFilter.ALL, 0.2f)
                .build());
        assertEquals(
            1.3f,
            out.build()
                .factor(FIRE, SWORD),
            1e-6);
    }

    @Test
    void boostScalesEachPartByItsOwnFactor() {
        ElementalDamage d = ElementalDamage.builder(DamageKind.PHYSICAL)
            .add(NEUTRAL, 6)
            .add(FIRE, 4)
            .build();
        Boosts b = Boosts.builder()
            .add(BoostFilter.element(FIRE), 0.5f)
            .build();
        ElementalDamage boosted = DamageMath.boost(d, b, SWORD);
        Map<Element, Float> parts = boosted.parts();
        assertEquals(6f, parts.get(NEUTRAL), 1e-6);
        assertEquals(6f, parts.get(FIRE), 1e-6);
        assertEquals(DamageKind.PHYSICAL, boosted.kind());
    }

    @Test
    void scaleTakesTheBoostedShareOfTheOriginalRaw() {
        ElementalDamage d = ElementalDamage.builder(DamageKind.PHYSICAL)
            .add(FIRE, 4)
            .build();
        Boosts b = Boosts.builder()
            .add(BoostFilter.ALL, 0.5f)
            .build();
        // Half of the hit reached the defender, as inside the hurt cooldown: half of the boosted 6.
        assertEquals(3f, DamageMath.scale(2f, d, DamageMath.boost(d, b, SWORD), Resistance.NONE), 1e-6);
    }

    @Test
    void boostComesBeforeTheFlatReduction() {
        ElementalDamage d = ElementalDamage.builder(DamageKind.PHYSICAL)
            .add(FIRE, 4)
            .build();
        Boosts b = Boosts.builder()
            .add(BoostFilter.ALL, 0.5f)
            .build();
        Resistance r = Resistance.builder()
            .subtract(FIRE, 2)
            .build();
        assertEquals(4f, DamageMath.resolve(DamageMath.boost(d, b, SWORD), r), 1e-6);
    }
}
