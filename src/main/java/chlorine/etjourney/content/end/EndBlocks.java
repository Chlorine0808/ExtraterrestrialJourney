package chlorine.etjourney.content.end;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import chlorine.etjourney.world.end.EndPalette;
import chlorine.etjourney.world.end.modifier.EndBlock;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Registers the End zone, biome, ore, plant and large-plant blocks, the ore drops and ingots, and binds the blocks into
 * the palette.
 */
public final class EndBlocks {

    private EndBlocks() {}

    public static void preInit() {
        for (ZoneBlockTable.Entry entry : ZoneBlockTable.ENTRIES) {
            ZoneBlock block = new ZoneBlock(entry);
            GameRegistry.registerBlock(block, entry.name);
            EndPalette.bind(entry.kind, block, 0);
        }
        Map<String, OreBlock> ores = new HashMap<>();
        for (OreBlockTable.Entry entry : OreBlockTable.ENTRIES) {
            MaterialItem drop = new MaterialItem(entry.drop);
            GameRegistry.registerItem(drop, entry.drop);
            OreBlock block = new OreBlock(entry, drop);
            GameRegistry.registerBlock(block, entry.name);
            EndPalette.bind(entry.kind, block, 0);
            OreDictionary.registerOre(entry.oreName, block);
            OreDictionary.registerOre(entry.dropOreName, drop);
            ores.put(entry.name, block);
        }
        registerIngots(ores);
        for (PlantBlockTable.Entry entry : PlantBlockTable.ENTRIES) {
            PlantBlock block = new PlantBlock(entry);
            GameRegistry.registerBlock(block, entry.name);
            EndPalette.bind(entry.kind, block, 0);
        }
        registerLargePlants();
    }

    /** Registers each ingot and lets its ore block and raw drop, or its raw material, smelt into it. */
    private static void registerIngots(Map<String, OreBlock> ores) {
        for (IngotTable.Entry entry : IngotTable.ENTRIES) {
            MaterialItem ingot = new MaterialItem(entry.name);
            GameRegistry.registerItem(ingot, entry.name);
            OreDictionary.registerOre(entry.oreName, ingot);
            if (entry.raw != null) {
                MaterialItem raw = new MaterialItem(entry.raw);
                GameRegistry.registerItem(raw, entry.raw);
                OreDictionary.registerOre(entry.rawOreName, raw);
                GameRegistry.addSmelting(raw, new ItemStack(ingot), entry.experience);
            }
            if (entry.ore == null) continue;
            OreBlock ore = ores.get(entry.ore);
            GameRegistry.addSmelting(ore, new ItemStack(ingot), entry.experience);
            GameRegistry.addSmelting(ore.getItemDropped(0, null, 0), new ItemStack(ingot), entry.experience);
        }
    }

    private static void registerLargePlants() {
        Map<String, Block> logs = new HashMap<>();
        MaterialItem shard = new MaterialItem(LargePlantBlockTable.SHARD);
        GameRegistry.registerItem(shard, LargePlantBlockTable.SHARD);
        // Saplings come first so the leaves can be told what they drop.
        Map<EndBlock, Item> saplings = new HashMap<>();
        for (LargePlantBlockTable.Sapling sapling : LargePlantBlockTable.SAPLINGS) {
            Block block = new EndSaplingBlock(sapling);
            GameRegistry.registerBlock(block, sapling.name);
            for (EndBlock leaves : sapling.droppedBy) saplings.put(leaves, Item.getItemFromBlock(block));
        }
        for (LargePlantBlockTable.Entry entry : LargePlantBlockTable.ENTRIES) {
            Block block;
            switch (entry.type) {
                case LOG:
                    block = new EndLogBlock(entry);
                    logs.put(entry.name, block);
                    break;
                case LEAVES:
                case GLOW_LEAVES:
                    EndLeavesBlock leaves = new EndLeavesBlock(entry);
                    leaves.setSapling(saplings.get(entry.kind));
                    block = leaves;
                    GameRegistry.registerBlock(block, EndLeavesBlock.Placed.class, entry.name);
                    EndPalette.bind(entry.kind, block, 0);
                    share(block, entry.type.oreName, entry.type.fireSpread, entry.type.flammability);
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
                case HANGING:
                    block = new EndHangingBlock(entry);
                    break;
                default:
                    continue;
            }
            GameRegistry.registerBlock(block, entry.name);
            EndPalette.bind(entry.kind, block, 0);
            share(block, entry.type.oreName, entry.type.fireSpread, entry.type.flammability);
        }
        for (LargePlantBlockTable.Wood wood : LargePlantBlockTable.WOODS) {
            Block planks = new EndPlanksBlock(wood.planks);
            GameRegistry.registerBlock(planks, wood.planks);
            share(
                planks,
                LargePlantBlockTable.PLANKS_ORE,
                LargePlantBlockTable.PLANKS_FIRE_SPREAD,
                LargePlantBlockTable.PLANKS_FLAMMABILITY);
            GameRegistry.addShapelessRecipe(new ItemStack(planks, 4), new ItemStack(logs.get(wood.log)));
        }
    }

    /** Lists the block under its ore dictionary name, any metadata, and lets it burn; a null name does neither. */
    private static void share(Block block, String oreName, int fireSpread, int flammability) {
        if (oreName == null) return;
        OreDictionary.registerOre(oreName, new ItemStack(block, 1, OreDictionary.WILDCARD_VALUE));
        Blocks.fire.setFireInfo(block, fireSpread, flammability);
    }
}
