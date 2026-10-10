package chlorine.etjourney.world.end;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import chlorine.etjourney.world.end.modifier.BlockSink;
import chlorine.etjourney.world.end.modifier.EndBlock;

/** Y 128-255 of a freshly generated chunk, written straight into its block storage, as a BlockSink. */
final class McStorageSink implements BlockSink {

    private final ExtendedBlockStorage[] storage;
    private final boolean hasSky;
    private final int originX, originZ;
    private boolean wrote;

    McStorageSink(ExtendedBlockStorage[] storage, boolean hasSky, int chunkX, int chunkZ) {
        this.storage = storage;
        this.hasSky = hasSky;
        this.originX = chunkX * 16;
        this.originZ = chunkZ * 16;
    }

    /** True once any block was placed, set or cleared. */
    boolean wrote() {
        return wrote;
    }

    @Override
    public int originX() {
        return originX;
    }

    @Override
    public int originZ() {
        return originZ;
    }

    @Override
    public int minY() {
        return TallPass.BASE_Y;
    }

    @Override
    public int maxY() {
        return 256;
    }

    @Override
    public boolean isAir(int x, int y, int z) {
        ExtendedBlockStorage section = storage[y >> 4];
        if (section == null) return true;
        Block b = section.getBlockByExtId(x - originX, y & 15, z - originZ);
        return b == null || b == Blocks.air;
    }

    @Override
    public void place(int x, int y, int z, EndBlock block) {
        if (isAir(x, y, z)) set(x, y, z, block);
    }

    @Override
    public void set(int x, int y, int z, EndBlock block) {
        put(x, y, z, EndPalette.block(block), EndPalette.meta(block));
    }

    @Override
    public void clear(int x, int y, int z) {
        if (storage[y >> 4] != null) put(x, y, z, Blocks.air, 0);
    }

    /** Writes a block, creating its 16-high section on demand. */
    void put(int x, int y, int z, Block block, int meta) {
        int index = y >> 4;
        if (storage[index] == null) storage[index] = new ExtendedBlockStorage(index << 4, hasSky);
        storage[index].func_150818_a(x - originX, y & 15, z - originZ, block);
        storage[index].setExtBlockMetadata(x - originX, y & 15, z - originZ, meta);
        wrote = true;
    }
}
