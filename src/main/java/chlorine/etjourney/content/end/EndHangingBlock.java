package chlorine.etjourney.content.end;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import chlorine.etjourney.core.ModInfo;
import chlorine.etjourney.core.registry.RegistryNames;

/** Glowing fur drawn crossed that hangs from a mycelia gill or from fur above, and drops nothing. */
final class EndHangingBlock extends Block {

    private static Block gill, vine;

    EndHangingBlock(LargePlantBlockTable.Entry entry) {
        super(Material.plants);
        setHardness(0.0F);
        setStepSound(soundTypeGrass);
        setLightLevel(entry.light / 15.0F);
        setCreativeTab(CreativeTabs.tabDecorations);
        setBlockName(RegistryNames.unlocalized(entry.name));
        setBlockTextureName(RegistryNames.texture(entry.name));
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public int getRenderType() {
        return 1;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        if (gill == null) {
            // Resolved on first use: this block is registered before the ones it hangs from.
            gill = Block.getBlockFromName(ModInfo.MODID + ":mycelia_gill");
            vine = Block.getBlockFromName(ModInfo.MODID + ":mycelia_vine");
        }
        Block above = world.getBlock(x, y + 1, z);
        return above == gill || above == vine;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (!canBlockStay(world, x, y, z)) world.setBlockToAir(x, y, z);
    }
}
