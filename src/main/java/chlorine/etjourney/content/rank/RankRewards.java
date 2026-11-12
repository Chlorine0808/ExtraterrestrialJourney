package chlorine.etjourney.content.rank;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.relauncher.ReflectionHelper;

/** Multiplies a ranked mob's drops and experience. */
final class RankRewards {

    private static final String[] EXPERIENCE = { "experienceValue", "field_70728_aV" };

    private RankRewards() {}

    static void multiplyDrops(List<EntityItem> drops, Rank rank) {
        for (EntityItem item : new ArrayList<>(drops)) {
            ItemStack stack = item.getEntityItem();
            if (stack == null) continue;
            List<Integer> sizes = RankLoot.stacks(stack.stackSize, rank.loot, stack.getMaxStackSize());
            if (sizes.isEmpty()) continue;
            stack.stackSize = sizes.get(0);
            for (int n : sizes.subList(1, sizes.size())) {
                ItemStack copy = stack.copy();
                copy.stackSize = n;
                EntityItem extra = new EntityItem(item.worldObj, item.posX, item.posY, item.posZ, copy);
                extra.delayBeforeCanPickup = 10;
                drops.add(extra);
            }
        }
    }

    /** Sets the experience the mob will drop; it is read when the death animation ends. */
    static void setExperience(EntityLiving mob, int factor) {
        int base = ReflectionHelper.getPrivateValue(EntityLiving.class, mob, EXPERIENCE);
        ReflectionHelper.setPrivateValue(EntityLiving.class, mob, base * factor, EXPERIENCE);
    }
}
