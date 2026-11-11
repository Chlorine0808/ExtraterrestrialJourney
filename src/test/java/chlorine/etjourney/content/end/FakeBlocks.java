package chlorine.etjourney.content.end;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

/** A world of air except for the blocks and metadata set on it. Blocks.* is null without a bootstrap. */
final class FakeBlocks implements IBlockAccess {

    static final Block AIR = new Block(Material.air) {};
    static final Block WOOD = new Block(Material.wood) {

        @Override
        public boolean canSustainLeaves(IBlockAccess world, int x, int y, int z) {
            return true;
        }
    };
    static final Block LEAVES = new Block(Material.leaves) {

        @Override
        public boolean isLeaves(IBlockAccess world, int x, int y, int z) {
            return true;
        }
    };

    private final Map<String, Block> blocks = new HashMap<>();
    private final Map<String, Integer> metas = new HashMap<>();

    FakeBlocks set(int x, int y, int z, Block block) {
        blocks.put(x + "," + y + "," + z, block);
        return this;
    }

    FakeBlocks meta(int x, int y, int z, int meta) {
        metas.put(x + "," + y + "," + z, meta);
        return this;
    }

    @Override
    public Block getBlock(int x, int y, int z) {
        Block b = blocks.get(x + "," + y + "," + z);
        return b == null ? AIR : b;
    }

    @Override
    public TileEntity getTileEntity(int x, int y, int z) {
        return null;
    }

    @Override
    public int getLightBrightnessForSkyBlocks(int x, int y, int z, int light) {
        return 0;
    }

    @Override
    public int getBlockMetadata(int x, int y, int z) {
        Integer m = metas.get(x + "," + y + "," + z);
        return m == null ? 0 : m;
    }

    @Override
    public int isBlockProvidingPowerTo(int x, int y, int z, int direction) {
        return 0;
    }

    @Override
    public boolean isAirBlock(int x, int y, int z) {
        return getBlock(x, y, z) == AIR;
    }

    @Override
    public BiomeGenBase getBiomeGenForCoords(int x, int z) {
        return null;
    }

    @Override
    public int getHeight() {
        return 256;
    }

    @Override
    public boolean extendedLevelsInChunkCache() {
        return false;
    }

    @Override
    public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean fallback) {
        return fallback;
    }
}
