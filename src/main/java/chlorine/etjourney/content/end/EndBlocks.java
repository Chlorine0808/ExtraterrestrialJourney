package chlorine.etjourney.content.end;

import chlorine.etjourney.world.end.EndPalette;
import cpw.mods.fml.common.registry.GameRegistry;

/** Registers the End zone, biome, ore and plant blocks and the ore drops, and binds the blocks into the palette. */
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
    }
}
