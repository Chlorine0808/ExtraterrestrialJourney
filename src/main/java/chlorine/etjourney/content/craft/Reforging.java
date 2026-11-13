package chlorine.etjourney.content.craft;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import chlorine.etjourney.core.growth.Gear;

/** The Forge's reforge: one piece of growing gear and two Ender Gold ingots draw its affixes again. */
final class Reforging {

    static final String COST_ORE = "ingotEnderGold";
    static final int COST = 2;
    static final int HITS = 8;
    /** Stands for the reforge in the Forge's progress; its output is never made. */
    static final Recipe RECIPE = Recipe.builder(new ItemOutput("minecraft:anvil", 0, 1), HITS)
        .build();

    private Reforging() {}

    /** The gear piece when the pile is exactly it and the cost, otherwise null. */
    static ItemStack find(List<ItemStack> pile) {
        ItemStack gear = null;
        int cost = 0;
        int ore = OreDictionary.getOreID(COST_ORE);
        for (ItemStack stack : pile) {
            if (stack == null) continue;
            if (Gear.slot(stack) != null && gear == null) gear = stack;
            else if (has(stack, ore)) cost += stack.stackSize;
            else return null;
        }
        return cost == COST ? gear : null;
    }

    private static boolean has(ItemStack stack, int ore) {
        for (int id : OreDictionary.getOreIDs(stack)) if (id == ore) return true;
        return false;
    }
}
