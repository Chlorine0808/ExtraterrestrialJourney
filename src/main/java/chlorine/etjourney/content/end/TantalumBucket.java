package chlorine.etjourney.content.end;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import chlorine.etjourney.core.registry.RegistryNames;

/** The bucket that holds hydrofluoric acid; EndAcid decides what the empty one takes up. */
final class TantalumBucket extends ItemBucket {

    private Item empty;

    TantalumBucket(Block contents, String name) {
        super(contents);
        setUnlocalizedName(RegistryNames.unlocalized(name));
        setTextureName(RegistryNames.texture(name));
        setCreativeTab(CreativeTabs.tabMisc);
    }

    /** What pouring the bucket out leaves in hand. */
    void setEmpty(Item empty) {
        this.empty = empty;
        setContainerItem(empty);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        ItemStack result = super.onItemRightClick(stack, world, player);
        // ItemBucket hands back a vanilla bucket once it has poured.
        return empty != null && result.getItem() == Items.bucket ? new ItemStack(empty) : result;
    }
}
