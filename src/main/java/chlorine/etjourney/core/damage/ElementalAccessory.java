package chlorine.etjourney.core.damage;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

import chlorine.etjourney.core.damage.model.Boosts;
import chlorine.etjourney.core.damage.model.Resistance;

/** Implemented by an accessory that gives elemental resistance or damage boosts while worn; never death immunity. */
public interface ElementalAccessory {

    default void addResistance(ItemStack stack, EntityLivingBase wearer, Resistance.Builder out) {}

    default void addBoosts(ItemStack stack, EntityLivingBase wearer, Boosts.Builder out) {}
}
