package chlorine.etjourney.core.combat.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.core.damage.model.AttackProfile;
import chlorine.etjourney.core.damage.model.DamageKind;
import chlorine.etjourney.core.damage.model.Reach;
import chlorine.etjourney.core.damage.model.WeaponClass;

class ConditionsTest {

    private static Situation at(float health, boolean night, AttackProfile attack) {
        return new Situation() {

            @Override
            public float healthFraction() {
                return health;
            }

            @Override
            public boolean isNight() {
                return night;
            }

            @Override
            public AttackProfile attack() {
                return attack;
            }
        };
    }

    private static final AttackProfile SWORD = new AttackProfile(
        DamageKind.PHYSICAL,
        Reach.MELEE,
        WeaponClass.SWORD,
        false);

    @Test
    void healthBoundsIncludeTheEdge() {
        assertTrue(
            Conditions.healthAtMost(0.5f)
                .test(at(0.5f, false, SWORD)));
        assertFalse(
            Conditions.healthAtMost(0.5f)
                .test(at(0.51f, false, SWORD)));
        assertTrue(
            Conditions.healthAtLeast(1f)
                .test(at(1f, false, SWORD)));
    }

    @Test
    void nightFollowsTheSituation() {
        assertTrue(
            Conditions.night()
                .test(at(1, true, SWORD)));
        assertFalse(
            Conditions.night()
                .test(at(1, false, SWORD)));
    }

    @Test
    void attackConditionsReadTheProfile() {
        assertTrue(
            Conditions.reach(Reach.MELEE)
                .test(at(1, false, SWORD)));
        assertFalse(
            Conditions.reach(Reach.MELEE)
                .test(at(1, false, AttackProfile.UNKNOWN)));
        assertTrue(
            Conditions.kind(DamageKind.PHYSICAL)
                .test(at(1, false, SWORD)));
    }

    @Test
    void combinatorsCombine() {
        Condition lowAtNight = Conditions.all(Conditions.night(), Conditions.healthAtMost(0.5f));
        assertTrue(lowAtNight.test(at(0.2f, true, SWORD)));
        assertFalse(lowAtNight.test(at(0.2f, false, SWORD)));
        Condition either = Conditions.any(Conditions.night(), Conditions.healthAtMost(0.5f));
        assertTrue(either.test(at(0.2f, false, SWORD)));
        assertFalse(either.test(at(0.9f, false, SWORD)));
        assertTrue(
            Conditions.not(Conditions.night())
                .test(at(1, false, SWORD)));
        assertTrue(
            Conditions.all()
                .test(at(1, false, SWORD)));
    }
}
