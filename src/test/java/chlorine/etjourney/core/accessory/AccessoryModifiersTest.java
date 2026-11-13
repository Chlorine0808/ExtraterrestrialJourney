package chlorine.etjourney.core.accessory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;

import net.minecraft.entity.SharedMonsterAttributes;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.core.accessory.model.ModifierPlan;

class AccessoryModifiersTest {

    @Test
    void differentAccessoriesOnOneAttributeKeepSeparateModifiers() {
        AccessoryModifiers out = new AccessoryModifiers();
        out.begin("etjourney:ring");
        out.add(SharedMonsterAttributes.attackDamage, 1, 0);
        out.begin("etjourney:charm");
        out.add(SharedMonsterAttributes.attackDamage, 1, 0);
        List<ModifierPlan.Entry> entries = out.entries();
        assertEquals(2, entries.size());
        assertNotEquals(entries.get(0).id, entries.get(1).id);
    }

    @Test
    void oneAccessoryAddingTwiceSumsIntoOneModifier() {
        AccessoryModifiers out = new AccessoryModifiers();
        out.begin("etjourney:ring");
        out.add(SharedMonsterAttributes.attackDamage, 1, 0);
        out.add(SharedMonsterAttributes.attackDamage, 2, 0);
        List<ModifierPlan.Entry> entries = out.entries();
        assertEquals(1, entries.size());
        assertEquals(3, entries.get(0).amount, 1e-9);
    }

    @Test
    void theSameAccessoryGetsTheSameIdEveryTime() {
        AccessoryModifiers first = new AccessoryModifiers(), second = new AccessoryModifiers();
        first.begin("etjourney:ring");
        first.add(SharedMonsterAttributes.movementSpeed, 0.05, 1);
        second.begin("etjourney:ring");
        second.add(SharedMonsterAttributes.movementSpeed, 0.05, 1);
        assertEquals(first.entries(), second.entries());
    }

    @Test
    void operationsOfOneAttributeAreSeparateModifiers() {
        AccessoryModifiers out = new AccessoryModifiers();
        out.begin("etjourney:ring");
        out.add(SharedMonsterAttributes.maxHealth, 2, 0);
        out.add(SharedMonsterAttributes.maxHealth, 0.1, 1);
        assertEquals(
            2,
            out.entries()
                .size());
    }
}
