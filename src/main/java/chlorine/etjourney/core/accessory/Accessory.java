package chlorine.etjourney.core.accessory;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

/** Implemented by an item worn in an accessory slot; ETJ's own events and ticks run its effects. */
public interface Accessory {

    /** Whether every worn copy counts; otherwise copies count once. */
    default boolean stacksWithCopies(ItemStack stack) {
        return false;
    }

    /** The attribute modifiers this accessory gives while it counts. */
    default void addModifiers(ItemStack stack, EntityLivingBase wearer, AccessoryModifiers out) {}
}
