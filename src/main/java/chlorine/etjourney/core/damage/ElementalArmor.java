package chlorine.etjourney.core.damage;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

import chlorine.etjourney.core.damage.model.Resistance;

/** Implemented by an armor item that gives elemental resistance while worn. */
public interface ElementalArmor {

    void addResistance(ItemStack stack, EntityLivingBase wearer, Resistance.Builder out);
}
