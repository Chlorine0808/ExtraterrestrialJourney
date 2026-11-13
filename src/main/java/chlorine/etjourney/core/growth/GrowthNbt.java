package chlorine.etjourney.core.growth;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import chlorine.etjourney.core.growth.model.AffixRoll;
import chlorine.etjourney.core.growth.model.GrowthData;
import chlorine.etjourney.core.growth.model.Rarity;

/** Reads and writes growth in a stack's NBT under etjourney.growth. */
public final class GrowthNbt {

    static final String KEY = "etjourney.growth";

    private GrowthNbt() {}

    /** Null when the stack has not been rolled, or its rarity is unknown. */
    public static GrowthData read(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()
            || !stack.getTagCompound()
                .hasKey(KEY))
            return null;
        NBTTagCompound tag = stack.getTagCompound()
            .getCompoundTag(KEY);
        Rarity rarity = Rarity.byName(tag.getString("rarity"));
        if (rarity == null) return null;
        List<AffixRoll> affixes = new ArrayList<>();
        NBTTagList list = tag.getTagList("affixes", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound a = list.getCompoundTagAt(i);
            affixes.add(new AffixRoll(a.getString("id"), a.getFloat("roll")));
        }
        return new GrowthData(tag.getInteger("xp"), rarity, affixes);
    }

    public static void write(ItemStack stack, GrowthData data) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("xp", data.xp);
        tag.setString("rarity", data.rarity.id);
        NBTTagList list = new NBTTagList();
        for (AffixRoll roll : data.affixes) {
            NBTTagCompound a = new NBTTagCompound();
            a.setString("id", roll.id);
            a.setFloat("roll", roll.roll);
            list.appendTag(a);
        }
        tag.setTag("affixes", list);
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound()
            .setTag(KEY, tag);
    }
}
