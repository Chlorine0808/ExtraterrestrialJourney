package chlorine.etjourney.core.damage.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.core.damage.model.WeaponClassifier.Direct;
import chlorine.etjourney.core.damage.model.WeaponClassifier.Facts;
import chlorine.etjourney.core.damage.model.WeaponClassifier.Held;

class WeaponClassifierTest {

    private static Facts melee(Held held) {
        return new Facts(Direct.SELF, held, null, false, false);
    }

    @Test
    void meleeTakesTheClassOfTheHeldItem() {
        assertEquals(WeaponClass.SWORD, WeaponClassifier.weapon(melee(Held.SWORD)));
        assertEquals(WeaponClass.AXE, WeaponClassifier.weapon(melee(Held.AXE)));
        assertEquals(WeaponClass.TOOL, WeaponClassifier.weapon(melee(Held.TOOL)));
        assertEquals(WeaponClass.UNARMED, WeaponClassifier.weapon(melee(Held.NONE)));
        assertEquals(WeaponClass.OTHER_MELEE, WeaponClassifier.weapon(melee(Held.BOW)));
        assertEquals(WeaponClass.OTHER_MELEE, WeaponClassifier.weapon(melee(Held.OTHER)));
        assertEquals(Reach.MELEE, WeaponClassifier.reach(melee(Held.SWORD)));
    }

    @Test
    void anArrowIsABowWhateverIsHeld() {
        Facts arrow = new Facts(Direct.ARROW, Held.SWORD, null, false, true);
        assertEquals(WeaponClass.BOW, WeaponClassifier.weapon(arrow));
        assertEquals(Reach.RANGED, WeaponClassifier.reach(arrow));
    }

    @Test
    void aMagicSwordStaysASword() {
        Facts magicSword = new Facts(Direct.SELF, Held.SWORD, null, true, false);
        assertEquals(WeaponClass.SWORD, WeaponClassifier.weapon(magicSword));
        assertEquals(Reach.MELEE, WeaponClassifier.reach(magicSword));
    }

    @Test
    void indirectMagicIsARangedSpell() {
        Facts potion = new Facts(Direct.OTHER, Held.NONE, null, true, false);
        assertEquals(WeaponClass.SPELL, WeaponClassifier.weapon(potion));
        assertEquals(Reach.RANGED, WeaponClassifier.reach(potion));
    }

    @Test
    void otherProjectilesAreThrown() {
        Facts snowball = new Facts(Direct.OTHER, Held.NONE, null, false, true);
        assertEquals(WeaponClass.THROWN, WeaponClassifier.weapon(snowball));
        assertEquals(Reach.RANGED, WeaponClassifier.reach(snowball));
    }

    @Test
    void noAttackerIsOther() {
        Facts none = new Facts(Direct.NONE, Held.NONE, null, false, false);
        assertEquals(WeaponClass.OTHER, WeaponClassifier.weapon(none));
        assertEquals(Reach.RANGED, WeaponClassifier.reach(none));
    }

    @Test
    void aDeclaredClassWinsInMelee() {
        Facts staff = new Facts(Direct.SELF, Held.OTHER, WeaponClass.SPELL, true, false);
        assertEquals(WeaponClass.SPELL, WeaponClassifier.weapon(staff));
        assertEquals(Reach.MELEE, WeaponClassifier.reach(staff));
    }
}
