package chlorine.etjourney.compat.baubles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import baubles.api.BaublesApi;
import chlorine.etjourney.core.accessory.WornAccessories;

/** Reads the worn accessories from a player's Baubles slots; other creatures have none. */
final class BaublesWorn implements WornAccessories.Provider {

    @Override
    public List<ItemStack> worn(EntityLivingBase living) {
        IInventory slots = slots(living);
        if (slots == null) return Collections.emptyList();
        List<ItemStack> worn = new ArrayList<>(slots.getSizeInventory());
        for (int i = 0; i < slots.getSizeInventory(); i++) worn.add(slots.getStackInSlot(i));
        return worn;
    }

    /** Setting the slot makes Baubles sync it to the clients. */
    @Override
    public boolean replace(EntityLivingBase living, ItemStack worn, ItemStack replacement) {
        IInventory slots = slots(living);
        if (slots == null) return false;
        for (int i = 0; i < slots.getSizeInventory(); i++) {
            if (slots.getStackInSlot(i) == worn) {
                slots.setInventorySlotContents(i, replacement);
                return true;
            }
        }
        return false;
    }

    private static IInventory slots(EntityLivingBase living) {
        return living instanceof EntityPlayer ? BaublesApi.getBaubles((EntityPlayer) living) : null;
    }
}
