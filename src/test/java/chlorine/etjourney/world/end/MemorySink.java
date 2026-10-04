package chlorine.etjourney.world.end;

import chlorine.etjourney.world.end.modifier.BlockSink;
import chlorine.etjourney.world.end.modifier.EndBlock;

/** A 16x16xheight chunk column array in memory, for block pass tests. */
final class MemorySink implements BlockSink {

    private final int originX, originZ, height;
    private final EndBlock[] blocks;

    MemorySink(int chunkX, int chunkZ, int height) {
        this.originX = chunkX * 16;
        this.originZ = chunkZ * 16;
        this.height = height;
        this.blocks = new EndBlock[16 * 16 * height];
    }

    private int index(int x, int y, int z) {
        return ((x - originX) * 16 + (z - originZ)) * height + y;
    }

    EndBlock get(int x, int y, int z) {
        return blocks[index(x, y, z)];
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
    public int height() {
        return height;
    }

    @Override
    public boolean isAir(int x, int y, int z) {
        return blocks[index(x, y, z)] == null;
    }

    @Override
    public void place(int x, int y, int z, EndBlock block) {
        if (isAir(x, y, z)) blocks[index(x, y, z)] = block;
    }

    @Override
    public void set(int x, int y, int z, EndBlock block) {
        blocks[index(x, y, z)] = block;
    }

    @Override
    public void clear(int x, int y, int z) {
        blocks[index(x, y, z)] = null;
    }
}
