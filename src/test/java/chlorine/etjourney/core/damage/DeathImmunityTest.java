package chlorine.etjourney.core.damage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.item.Item;
import net.minecraft.util.DamageSource;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.core.damage.model.DamageKind;
import chlorine.etjourney.core.damage.model.Element;
import chlorine.etjourney.core.damage.model.ElementRegistry;
import chlorine.etjourney.core.damage.model.ElementalDamage;

class DeathImmunityTest {

    private static final class ImmuneItem extends Item implements DeathImmunity.Armor {
    }

    private static final Element VOID = ElementRegistry.registerAbsolute("test_immunity_void");

    private static ElementalDamageSource elemental(boolean piercing) {
        ElementalDamage damage = ElementalDamage.builder(DamageKind.MAGIC)
            .add(piercing ? VOID : ElementRegistry.NEUTRAL, 5)
            .build();
        return new ElementalDamageSource(damage, null, null);
    }

    @Test
    void letsOnlyPiercingAndTheVoidThrough() {
        assertTrue(DeathImmunity.letsThrough(elemental(true)));
        assertTrue(DeathImmunity.letsThrough(DamageSource.outOfWorld));
        assertFalse(DeathImmunity.letsThrough(elemental(false)));
        assertFalse(DeathImmunity.letsThrough(DamageSource.generic));
        assertFalse(DeathImmunity.letsThrough(DamageSource.magic));
    }

    @Test
    void needsEveryPieceToBeImmune() {
        Item immune = new ImmuneItem(), plain = new Item();
        assertTrue(DeathImmunity.fullSet(immune, immune, immune, immune));
        assertFalse(DeathImmunity.fullSet(immune, immune, immune, plain));
        assertFalse(DeathImmunity.fullSet(immune, immune, immune, null));
    }
}
