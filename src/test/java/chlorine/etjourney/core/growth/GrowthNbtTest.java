package chlorine.etjourney.core.growth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.core.growth.model.AffixRoll;
import chlorine.etjourney.core.growth.model.GrowthData;
import chlorine.etjourney.core.growth.model.Rarity;

class GrowthNbtTest {

    @Test
    void roundTripsKeepingOtherTags() {
        ItemStack stack = new ItemStack(new Item());
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("other", 3);
        stack.setTagCompound(tag);
        GrowthNbt.write(
            stack,
            new GrowthData(
                120,
                Rarity.EPIC,
                Arrays.asList(new AffixRoll("keen", 0.6f), new AffixRoll("brutal", 0.9f))));
        GrowthData back = GrowthNbt.read(stack);
        assertEquals(120, back.xp);
        assertEquals(Rarity.EPIC, back.rarity);
        assertEquals(2, back.affixes.size());
        assertEquals("brutal", back.affixes.get(1).id);
        assertEquals(0.9f, back.affixes.get(1).roll, 1e-6);
        assertEquals(
            3,
            stack.getTagCompound()
                .getInteger("other"));
    }

    @Test
    void unrolledOrUnknownIsNull() {
        ItemStack stack = new ItemStack(new Item());
        assertNull(GrowthNbt.read(stack));
        NBTTagCompound growth = new NBTTagCompound();
        growth.setString("rarity", "mythic");
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag(GrowthNbt.KEY, growth);
        stack.setTagCompound(tag);
        assertNull(GrowthNbt.read(stack));
    }
}
