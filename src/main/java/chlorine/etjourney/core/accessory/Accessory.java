package chlorine.etjourney.core.accessory;

import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

import chlorine.etjourney.core.combat.CombatHook;

/** Implemented by an item worn in an accessory slot; ETJ's own events and ticks run its effects. */
public interface Accessory {

    /** Whether every worn copy counts; otherwise copies count once. */
    default boolean stacksWithCopies(ItemStack stack) {
        return false;
    }

    /** The attribute modifiers this accessory gives while it counts. */
    default void addModifiers(ItemStack stack, EntityLivingBase wearer, AccessoryModifiers out) {}

    /** The combat hooks this accessory gives while it counts. */
    default void addHooks(ItemStack stack, EntityLivingBase wearer, List<CombatHook> out) {}

    /**
     * Runs once a second on the server while it counts. Return the same stack to leave the slot alone, or a
     * replacement, null to empty the slot.
     */
    default ItemStack tickWorn(ItemStack stack, EntityLivingBase wearer) {
        return stack;
    }
}
