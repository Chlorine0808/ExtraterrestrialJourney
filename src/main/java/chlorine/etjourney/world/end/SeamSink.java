package chlorine.etjourney.world.end;

import chlorine.etjourney.world.end.modifier.BlockSink;
import chlorine.etjourney.world.end.modifier.EndBlock;

/** A short stack of blocks just above the generator's Y 127, kept in memory so the lower half can read it. */
final class SeamSink implements BlockSink {

    static final int HEIGHT = 32;

    private final int originX, originZ;
    private final EndBlock[] blocks = new EndBlock[16 * 16 * HEIGHT];

    SeamSink(int chunkX, int chunkZ) {
        this.originX = chunkX * 16;
        this.originZ = chunkZ * 16;
    }

    private int index(int x, int y, int z) {
        int lx = x - originX, lz = z - originZ, ly = y - TallPass.BASE_Y;
        if (lx < 0 || lx >= 16 || lz < 0 || lz >= 16 || ly < 0 || ly >= HEIGHT) return -1;
        return (lx * 16 + lz) * HEIGHT + ly;
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
        return TallPass.BASE_Y + HEIGHT;
    }

    @Override
    public boolean isAir(int x, int y, int z) {
        int i = index(x, y, z);
        return i < 0 || blocks[i] == null;
    }

    @Override
    public void place(int x, int y, int z, EndBlock block) {
        int i = index(x, y, z);
        if (i >= 0 && blocks[i] == null) blocks[i] = block;
    }

    @Override
    public void set(int x, int y, int z, EndBlock block) {
        int i = index(x, y, z);
        if (i >= 0) blocks[i] = block;
    }

    @Override
    public void clear(int x, int y, int z) {
        int i = index(x, y, z);
        if (i >= 0) blocks[i] = null;
    }
}
