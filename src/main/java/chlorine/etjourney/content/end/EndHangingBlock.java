package chlorine.etjourney.content.end;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import chlorine.etjourney.core.ModInfo;
import chlorine.etjourney.core.registry.RegistryNames;

/** Glowing fur or vine drawn crossed that hangs from the blocks its entry names, and drops nothing. */
final class EndHangingBlock extends Block {

    private final List<String> hangsFrom;
    private List<Block> supports;

    EndHangingBlock(LargePlantBlockTable.Entry entry) {
        super(Material.plants);
        setHardness(0.0F);
        setStepSound(soundTypeGrass);
        setLightLevel(entry.light / 15.0F);
        setCreativeTab(CreativeTabs.tabDecorations);
        setBlockName(RegistryNames.unlocalized(entry.name));
        setBlockTextureName(RegistryNames.texture(entry.name));
        hangsFrom = entry.hangsFrom;
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
        if (supports == null) {
            // Resolved on first use: this block may be registered before the ones it hangs from.
            List<Block> found = new ArrayList<>();
            for (String name : hangsFrom) found.add(Block.getBlockFromName(ModInfo.MODID + ":" + name));
            supports = found;
        }
        return supports.contains(world.getBlock(x, y + 1, z));
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (!canBlockStay(world, x, y, z)) world.setBlockToAir(x, y, z);
    }
}
