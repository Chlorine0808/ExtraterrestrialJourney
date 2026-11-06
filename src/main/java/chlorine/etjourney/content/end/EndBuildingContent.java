package chlorine.etjourney.content.end;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.ChestGenHooks;

import chlorine.etjourney.world.building.BlockRef;
import chlorine.etjourney.world.end.building.BuildingKind;
import chlorine.etjourney.world.end.building.EndBuildings;

/** The End buildings and their chest loot. */
public final class EndBuildingContent {

    public static final String RUINS_LOOT = "etjourney:end_ruins";

    private EndBuildingContent() {}

    public static void init() {
        ChestGenHooks ruins = ChestGenHooks.getInfo(RUINS_LOOT);
        ruins.setMin(3);
        ruins.setMax(6);
        ruins.addItem(new WeightedRandomChestContent(new ItemStack(Items.ender_pearl), 1, 3, 10));
        ruins.addItem(new WeightedRandomChestContent(new ItemStack(Items.iron_ingot), 1, 5, 10));
        ruins.addItem(new WeightedRandomChestContent(new ItemStack(Items.gold_ingot), 1, 3, 5));
        ruins.addItem(new WeightedRandomChestContent(new ItemStack(Items.bread), 1, 3, 10));

        // Test buildings for the framework; common so they are easy to find. Remove once real buildings exist.
        EndBuildings.add(
            BuildingKind.builder("test_box", "test_box")
                .cell(192)
                .chance(0.5)
                .standing(6, 2)
                .foundation(new BlockRef("minecraft:end_stone", 0, null), 24)
                .build());
        EndBuildings.add(
            BuildingKind.builder("test_tower", "test_tower")
                .cell(256)
                .chance(0.3)
                .floating(220, 280)
                .build());
    }
}
