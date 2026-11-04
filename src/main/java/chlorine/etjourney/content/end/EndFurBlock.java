package chlorine.etjourney.content.end;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import chlorine.etjourney.core.ModInfo;
import chlorine.etjourney.core.registry.RegistryNames;

/** Outer leaves drawn crossed beside the leaves their entry names; drops nothing and goes when no leaf touches it. */
final class EndFurBlock extends Block {

    private static final int[][] FACES = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 },
        { 0, 0, -1 } };

    private final String leavesName;
    private Block leaves;

    EndFurBlock(LargePlantBlockTable.Entry entry) {
        super(Material.leaves);
        setHardness(0.0F);
        setStepSound(soundTypeGrass);
        setLightLevel(entry.light / 15.0F);
        setCreativeTab(CreativeTabs.tabDecorations);
        setBlockName(RegistryNames.unlocalized(entry.name));
        setBlockTextureName(RegistryNames.texture(entry.name));
        leavesName = entry.hangsFrom.get(0);
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
    public Item getItemDropped(int meta, Random random, int fortune) {
        return null;
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        // Resolved on first use: this block may be registered before its leaves.
        if (leaves == null) leaves = Block.getBlockFromName(ModInfo.MODID + ":" + leavesName);
        for (int[] f : FACES) if (world.getBlock(x + f[0], y + f[1], z + f[2]) == leaves) return true;
        return false;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (!canBlockStay(world, x, y, z)) world.setBlockToAir(x, y, z);
    }
}
