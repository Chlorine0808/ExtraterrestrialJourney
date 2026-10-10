package chlorine.etjourney.world.end;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import chlorine.etjourney.world.end.modifier.BiomePart;
import chlorine.etjourney.world.end.modifier.BlockSink;
import chlorine.etjourney.world.end.modifier.EndBlock;

/** The generator's 16x16x128 block array (air is null) seen as a BlockSink in world coordinates. */
final class McBlockSink implements BlockSink {

    private static final int HEIGHT = 128;

    private final Block[] blocks;
    private final byte[] meta;
    private final int originX, originZ;

    McBlockSink(Block[] blocks, byte[] meta, int chunkX, int chunkZ) {
        this.blocks = blocks;
        this.meta = meta != null && meta.length == blocks.length ? meta : null;
        this.originX = chunkX * 16;
        this.originZ = chunkZ * 16;
    }

    private int index(int x, int y, int z) {
        return ((x - originX) * 16 + (z - originZ)) * HEIGHT + y;
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
        return 0;
    }

    @Override
    public int maxY() {
        return HEIGHT;
    }

    @Override
    public boolean isAir(int x, int y, int z) {
        Block b = blocks[index(x, y, z)];
        return b == null || b == Blocks.air;
    }

    @Override
    public void place(int x, int y, int z, EndBlock block) {
        if (isAir(x, y, z)) set(x, y, z, block);
    }

    @Override
    public void set(int x, int y, int z, EndBlock block) {
        Block b = EndPalette.block(block);
        int m = EndPalette.meta(block);
        // Without a metadata array, coloured clay falls back to plain hardened clay.
        if (m != 0 && meta == null) b = Blocks.hardened_clay;
        put(x, y, z, b, m);
    }

    @Override
    public void setBiome(int x, int y, int z, BiomePart part, double pick) {
        put(x, y, z, BiomePalette.block(part, pick), BiomePalette.meta(part, pick));
    }

    private void put(int x, int y, int z, Block b, int m) {
        int i = index(x, y, z);
        blocks[i] = b;
        if (meta != null) meta[i] = (byte) m;
    }

    @Override
    public void clear(int x, int y, int z) {
        int i = index(x, y, z);
        blocks[i] = null;
        if (meta != null) meta[i] = 0;
    }
}
