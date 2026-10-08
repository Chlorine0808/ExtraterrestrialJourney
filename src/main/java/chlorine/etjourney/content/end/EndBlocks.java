package chlorine.etjourney.content.end;

import chlorine.etjourney.world.end.EndPalette;
import cpw.mods.fml.common.registry.GameRegistry;

/** Registers the End zone blocks and binds them into the terrain palette. */
public final class EndBlocks {

    private EndBlocks() {}

    public static void preInit() {
        for (ZoneBlockTable.Entry entry : ZoneBlockTable.ENTRIES) {
            ZoneBlock block = new ZoneBlock(entry);
            GameRegistry.registerBlock(block, entry.name);
            EndPalette.bind(entry.kind, block, 0);
        }
    }
}
