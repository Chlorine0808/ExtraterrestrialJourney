package chlorine.etjourney.world.end;

import chlorine.etjourney.world.end.modifier.BlockSink;
import chlorine.etjourney.world.end.modifier.EndBlock;

/** A 16x16 column array in memory covering Y minY to maxY - 1, for block pass tests. */
final class MemorySink implements BlockSink {

    private final int originX, originZ, minY, maxY;
    private final EndBlock[] blocks;
    private boolean outOfRange;

    MemorySink(int chunkX, int chunkZ, int height) {
        this(chunkX, chunkZ, 0, height);
    }

    MemorySink(int chunkX, int chunkZ, int minY, int maxY) {
        this.originX = chunkX * 16;
        this.originZ = chunkZ * 16;
        this.minY = minY;
        this.maxY = maxY;
        this.blocks = new EndBlock[16 * 16 * (maxY - minY)];
    }

    private int index(int x, int y, int z) {
        if (y < minY || y >= maxY || x < originX || x >= originX + 16 || z < originZ || z >= originZ + 16) {
            outOfRange = true;
            throw new IllegalArgumentException("outside the sink: " + x + "," + y + "," + z);
        }
        return ((x - originX) * 16 + (z - originZ)) * (maxY - minY) + y - minY;
    }

    /** True when no modifier ever touched a position outside the sink. */
    boolean placedOnlyWithinRange() {
        return !outOfRange;
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
    public int minY() {
        return minY;
    }

    @Override
    public int maxY() {
        return maxY;
    }

    @Override
    public boolean isAir(int x, int y, int z) {
        return blocks[index(x, y, z)] == null;
    }

    @Override
    public void place(int x, int y, int z, EndBlock block) {
        if (isAir(x, y, z)) set(x, y, z, block);
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
