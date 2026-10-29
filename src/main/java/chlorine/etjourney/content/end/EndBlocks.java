package chlorine.etjourney.content.end;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import chlorine.etjourney.world.end.EndPalette;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Registers the End zone, biome, ore, plant and large-plant blocks and the ore drops, and binds the blocks into the
 * palette.
 */
public final class EndBlocks {

    private EndBlocks() {}

    public static void preInit() {
        for (ZoneBlockTable.Entry entry : ZoneBlockTable.ENTRIES) {
            ZoneBlock block = new ZoneBlock(entry);
            GameRegistry.registerBlock(block, entry.name);
            EndPalette.bind(entry.kind, block, 0);
        }
        for (OreBlockTable.Entry entry : OreBlockTable.ENTRIES) {
            MaterialItem drop = new MaterialItem(entry.drop);
            GameRegistry.registerItem(drop, entry.drop);
            OreBlock block = new OreBlock(entry, drop);
            GameRegistry.registerBlock(block, entry.name);
            EndPalette.bind(entry.kind, block, 0);
        }
        for (PlantBlockTable.Entry entry : PlantBlockTable.ENTRIES) {
            PlantBlock block = new PlantBlock(entry);
            GameRegistry.registerBlock(block, entry.name);
            EndPalette.bind(entry.kind, block, 0);
        }
        registerLargePlants();
    }

    private static void registerLargePlants() {
        Map<String, Block> logs = new HashMap<>();
        MaterialItem shard = new MaterialItem(LargePlantBlockTable.SHARD);
        GameRegistry.registerItem(shard, LargePlantBlockTable.SHARD);
        for (LargePlantBlockTable.Entry entry : LargePlantBlockTable.ENTRIES) {
            Block block;
            switch (entry.type) {
                case LOG:
                    block = new EndLogBlock(entry);
                    logs.put(entry.name, block);
                    break;
                case LEAVES:
                case GLOW_LEAVES:
                    block = new EndLeavesBlock(entry);
                    GameRegistry.registerBlock(block, EndLeavesBlock.Placed.class, entry.name);
                    EndPalette.bind(entry.kind, block, 0);
                    continue;
                case CAP:
                case GLOW:
                case STEM:
                    block = new EndCapBlock(entry);
                    break;
                case CRYSTAL:
                    block = new CrystalBlock(entry);
                    break;
                case BUDDING:
                    block = new BuddingCrystalBlock(entry);
                    break;
                case BUD:
                    block = new CrystalBudBlock(entry, shard);
                    // Generation sets buds pointing up.
                    GameRegistry.registerBlock(block, entry.name);
                    EndPalette.bind(entry.kind, block, 1);
                    continue;
                default:
                    continue;
            }
            GameRegistry.registerBlock(block, entry.name);
            EndPalette.bind(entry.kind, block, 0);
        }
        for (LargePlantBlockTable.Wood wood : LargePlantBlockTable.WOODS) {
            Block planks = new EndPlanksBlock(wood.planks);
            GameRegistry.registerBlock(planks, wood.planks);
            GameRegistry.addShapelessRecipe(new ItemStack(planks, 4), new ItemStack(logs.get(wood.log)));
        }
    }
}
