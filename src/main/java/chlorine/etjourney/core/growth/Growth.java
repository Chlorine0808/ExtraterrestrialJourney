package chlorine.etjourney.core.growth;

import java.util.Random;

import net.minecraft.item.ItemStack;

import chlorine.etjourney.core.growth.model.GearSlot;
import chlorine.etjourney.core.growth.model.GrowthData;
import chlorine.etjourney.core.growth.model.Reforge;

/** The way in for growth: rolling, experience and reforging, all in the stack's NBT. */
public final class Growth {

    private Growth() {}

    /** Rolls the stack when it grows but has no growth yet; null when it does not grow. */
    public static GrowthData ensureRolled(ItemStack stack, Random random) {
        GearSlot slot = Gear.slot(stack);
        if (slot == null) return null;
        GrowthData data = GrowthNbt.read(stack);
        if (data == null) {
            data = GrowthData.roll(Affixes.pool(slot), random);
            GrowthNbt.write(stack, data);
        }
        return data;
    }

    /** Adds experience in place; returns the new level when it rose, otherwise -1. */
    public static int gainXp(ItemStack stack, int xp, Random random) {
        GrowthData data = ensureRolled(stack, random);
        if (data == null) return -1;
        GrowthData next = data.withXp(xp);
        GrowthNbt.write(stack, next);
        return next.level() > data.level() ? next.level() : -1;
    }

    /** Draws the affixes again in place; false when the stack does not grow. */
    public static boolean reforge(ItemStack stack, Random random) {
        GearSlot slot = Gear.slot(stack);
        if (slot == null) return false;
        GrowthNbt.write(stack, Reforge.reroll(GrowthNbt.read(stack), Affixes.pool(slot), random));
        return true;
    }
}
