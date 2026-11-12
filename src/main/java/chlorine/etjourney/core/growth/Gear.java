package chlorine.etjourney.core.growth;

import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;

import chlorine.etjourney.core.accessory.Accessory;
import chlorine.etjourney.core.damage.WeaponClassified;
import chlorine.etjourney.core.growth.model.GearSlot;

/** Tells which kind of growing gear an item is, if any; stackable items never grow. */
public final class Gear {

    private Gear() {}

    /** Null for anything that does not grow. */
    public static GearSlot slot(ItemStack stack) {
        if (stack == null || stack.getItem() == null || stack.getMaxStackSize() > 1) return null;
        Item item = stack.getItem();
        if (item instanceof Accessory) return GearSlot.ACCESSORY;
        if (item instanceof ItemArmor) return GearSlot.ARMOR;
        if (item instanceof ItemSword || item instanceof ItemAxe
            || item instanceof ItemBow
            || item instanceof WeaponClassified) return GearSlot.WEAPON;
        return null;
    }
}
