package chlorine.etjourney.core.damage;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

import chlorine.etjourney.core.damage.model.Resistance;

/** Implemented by an accessory item that gives elemental resistance while worn; never death immunity. */
public interface ElementalAccessory {

    void addResistance(ItemStack stack, EntityLivingBase wearer, Resistance.Builder out);
}
