package chlorine.etjourney.content.end;

import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.modifier.EndBlock;

/**
 * Buds stay on a face only while it is solid. Glass material is not opaque, so Forge's default isSideSolid would
 * call every crystal face open and every bud would pop on the first neighbour update.
 */
class CrystalBlockTest {

    /** An empty world: air everywhere, metadata 0. */
    private static final IBlockAccess EMPTY = new IBlockAccess() {

        @Override
        public Block getBlock(int x, int y, int z) {
            return Blocks.air;
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
            return 0;
        }

        @Override
        public int isBlockProvidingPowerTo(int x, int y, int z, int direction) {
            return 0;
        }

        @Override
        public boolean isAirBlock(int x, int y, int z) {
            return true;
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
    };

    private static LargePlantBlockTable.Entry entry(EndBlock kind) {
        for (LargePlantBlockTable.Entry e : LargePlantBlockTable.ENTRIES) {
            if (e.kind == kind) return e;
        }
        throw new IllegalArgumentException(kind.toString());
    }

    @Test
    void everyCrystalFaceHoldsABud() {
        Block[] crystals = { new CrystalBlock(entry(EndBlock.CRYSTAL)),
            new BuddingCrystalBlock(entry(EndBlock.BUDDING_CRYSTAL)) };
        for (Block crystal : crystals) {
            for (ForgeDirection face : ForgeDirection.VALID_DIRECTIONS) {
                assertTrue(crystal.isSideSolid(EMPTY, 0, 64, 0, face), crystal + " is open on " + face);
            }
        }
    }
}
