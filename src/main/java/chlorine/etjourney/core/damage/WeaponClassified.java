package chlorine.etjourney.core.damage;

import net.minecraft.item.ItemStack;

import chlorine.etjourney.core.damage.model.WeaponClass;

/** Implemented by an item that names its own weapon class, such as a staff vanilla cannot tell apart. */
public interface WeaponClassified {

    WeaponClass weaponClass(ItemStack stack);
}
