package chlorine.etjourney.content.craft;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import chlorine.etjourney.core.registry.RegistryNames;

/**
 * A device that items are placed on by hand. Right-click with an item to place one; on the Forge an empty hand
 * hits and sneaking with an empty hand takes the last item back, elsewhere an empty hand takes it back.
 */
final class CraftDeviceBlock extends BlockContainer {

    enum Kind {

        FORGE("forge", Material.iron),
        ALTAR("attune_altar", Material.rock),
        PEDESTAL("attune_pedestal", Material.rock);

        final String name;
        final Material material;

        Kind(String name, Material material) {
            this.name = name;
            this.material = material;
        }
    }

    private final Kind kind;

    CraftDeviceBlock(Kind kind) {
        super(kind.material);
        this.kind = kind;
        setHardness(3.0F);
        setResistance(10.0F);
        setStepSound(kind == Kind.FORGE ? soundTypeAnvil : soundTypePiston);
        setHarvestLevel("pickaxe", 0);
        setCreativeTab(CreativeTabs.tabDecorations);
        setBlockName(RegistryNames.unlocalized(kind.name));
        setBlockTextureName(RegistryNames.texture(kind.name));
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        switch (kind) {
            case FORGE:
                return new ForgeTile();
            case ALTAR:
                return new AltarTile();
            default:
                return new PedestalTile();
        }
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (world.isRemote) return true;
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof ItemHolderTile)) return false;
        ItemHolderTile holder = (ItemHolderTile) tile;
        ItemStack held = player.getCurrentEquippedItem();
        if (held != null) {
            if (holder.place(held) && held.stackSize <= 0) player.destroyCurrentEquippedItem();
            return true;
        }
        if (kind == Kind.FORGE && !player.isSneaking()) {
            ((ForgeTile) holder).hit();
            return true;
        }
        ItemStack taken = holder.takeLast();
        if (taken != null && !player.inventory.addItemStackToInventory(taken)) holder.spawn(taken);
        return true;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof ItemHolderTile) ((ItemHolderTile) tile).dropAll();
        super.breakBlock(world, x, y, z, block, meta);
    }
}
