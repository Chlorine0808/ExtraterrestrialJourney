package chlorine.etjourney.core.status;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

import chlorine.etjourney.core.status.model.StatusBoosts;
import chlorine.etjourney.core.status.model.StatusResistance;

/** Implemented by armor or an accessory that changes status effects while worn. */
public interface StatusWard {

    default void addStatusResistance(ItemStack stack, EntityLivingBase wearer, StatusResistance.Builder out) {}

    default void addStatusBoosts(ItemStack stack, EntityLivingBase wearer, StatusBoosts.Builder out) {}
}
