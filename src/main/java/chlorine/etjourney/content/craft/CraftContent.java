package chlorine.etjourney.content.craft;

import chlorine.etjourney.core.ModInfo;
import cpw.mods.fml.common.registry.GameRegistry;

/** Registers the Forge and the Attune altar and pedestal. */
public final class CraftContent {

    private CraftContent() {}

    public static void preInit() {
        for (CraftDeviceBlock.Kind kind : CraftDeviceBlock.Kind.values()) {
            GameRegistry.registerBlock(new CraftDeviceBlock(kind), kind.name);
        }
        GameRegistry.registerTileEntity(ForgeTile.class, ModInfo.MODID + ":forge");
        GameRegistry.registerTileEntity(AltarTile.class, ModInfo.MODID + ":attune_altar");
        GameRegistry.registerTileEntity(PedestalTile.class, ModInfo.MODID + ":attune_pedestal");
    }
}
