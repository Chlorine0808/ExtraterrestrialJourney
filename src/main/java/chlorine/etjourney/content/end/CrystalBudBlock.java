package chlorine.etjourney.content.end;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import chlorine.etjourney.core.registry.RegistryNames;
import chlorine.etjourney.world.end.feature.plant.CrystalGrowth;
import chlorine.etjourney.world.end.modifier.EndBlock;

/**
 * A crystal bud or cluster pointing out of a face: meta 0-5 is the facing in ForgeDirection order, and the block
 * breaks when the face it grows from stops being solid. Only the cluster drops anything, one shard.
 */
final class CrystalBudBlock extends Block {

    /** Width and height in sixteenths of a block, for the small, medium and large bud and the cluster. */
    private static final int[][] SIZES = { { 8, 3 }, { 10, 4 }, { 12, 5 }, { 14, 7 } };

    private final boolean dropsShard;
    private final Item shard;
    private final int[] size;

    CrystalBudBlock(LargePlantBlockTable.Entry entry, Item shard) {
        super(Material.glass);
        this.dropsShard = CrystalGrowth.dropsShard(entry.kind);
        this.shard = shard;
        this.size = SIZES[stage(entry.kind)];
        setHardness(1.5F);
        setStepSound(soundTypeGlass);
        setLightLevel(entry.light / 15.0F);
        setCreativeTab(CreativeTabs.tabDecorations);
        setBlockName(RegistryNames.unlocalized(entry.name));
        setBlockTextureName(RegistryNames.texture(entry.name));
    }

    private static int stage(EndBlock kind) {
        switch (kind) {
            case CRYSTAL_BUD_SMALL:
                return 0;
            case CRYSTAL_BUD_MEDIUM:
                return 1;
            case CRYSTAL_BUD_LARGE:
                return 2;
            default:
                return 3;
        }
    }

    @Override
    public int getRenderType() {
        return 1;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    /** A placed bud points away from the face it was set on. */
    @Override
    public int onBlockPlaced(World world, int x, int y, int z, int side, float hx, float hy, float hz, int meta) {
        return side;
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        float half = size[0] / 32.0F;
        float lo = 0.5F - half, hi = 0.5F + half;
        float length = size[1] / 16.0F;
        switch (world.getBlockMetadata(x, y, z) % 6) {
            case 0:
                setBlockBounds(lo, 1.0F - length, lo, hi, 1.0F, hi);
                break;
            case 2:
                setBlockBounds(lo, lo, 1.0F - length, hi, hi, 1.0F);
                break;
            case 3:
                setBlockBounds(lo, lo, 0.0F, hi, hi, length);
                break;
            case 4:
                setBlockBounds(1.0F - length, lo, lo, 1.0F, hi, hi);
                break;
            case 5:
                setBlockBounds(0.0F, lo, lo, length, hi, hi);
                break;
            default:
                setBlockBounds(lo, 0.0F, lo, hi, length, hi);
        }
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        setBlockBoundsBasedOnState(world, x, y, z);
        return super.getCollisionBoundingBoxFromPool(world, x, y, z);
    }

    @Override
    public boolean canPlaceBlockOnSide(World world, int x, int y, int z, int side) {
        return isSupported(world, x, y, z, side);
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        return isSupported(world, x, y, z, world.getBlockMetadata(x, y, z) % 6);
    }

    /** Whether the neighbour on the facing's support side is solid on the face that touches the bud. */
    private static boolean isSupported(World world, int x, int y, int z, int facing) {
        ForgeDirection support = ForgeDirection.getOrientation(CrystalGrowth.supportSide(facing));
        return world.isSideSolid(
            x + support.offsetX,
            y + support.offsetY,
            z + support.offsetZ,
            ForgeDirection.getOrientation(facing));
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbour) {
        if (canBlockStay(world, x, y, z)) return;
        // Break it as a player would, so a cluster still drops its shard.
        world.func_147480_a(x, y, z, true);
    }

    @Override
    public int quantityDropped(Random random) {
        return dropsShard ? 1 : 0;
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return dropsShard ? shard : null;
    }

    @Override
    protected boolean canSilkHarvest() {
        return false;
    }
}
