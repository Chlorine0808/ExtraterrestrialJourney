package chlorine.etjourney.content;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;

import chlorine.etjourney.core.ModInfo;
import cpw.mods.fml.common.registry.GameRegistry;

/** The one creative tab every ETJourney block and item lives in. */
public final class ModCreativeTab extends CreativeTabs {

    public static final CreativeTabs TAB = new ModCreativeTab();

    private static final String ICON = "crystal_shard";

    private ModCreativeTab() {
        super(ModInfo.MODID);
    }

    @Override
    public Item getTabIconItem() {
        Item icon = GameRegistry.findItem(ModInfo.MODID, ICON);
        return icon != null ? icon : Items.ender_eye;
    }
}
