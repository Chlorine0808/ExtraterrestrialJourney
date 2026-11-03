package chlorine.etjourney.world.end.feature.plant;

import chlorine.etjourney.world.end.modifier.BlockSink;
import chlorine.etjourney.world.end.modifier.EndBlock;

/**
 * One run of blocks hanging from a placed block, collected so it can be judged whole: it belongs to the half (below or
 * above {@link #SPLIT_Y}) that holds the block it hangs from, its cells in the other half are cut, and it is written
 * only when that block was placed and every cell of it in the sink is air. So both halves, and any chunk, agree.
 */
final class HangingRun {

    /** The y at which the block passes split. */
    static final int SPLIT_Y = 128;

    private final BlockSink sink;
    private final int[] xs, ys, zs;
    private final EndBlock[] kinds;
    private int n, half;
    private boolean anchorPlaced;

    HangingRun(BlockSink sink, int capacity) {
        this.sink = sink;
        xs = new int[capacity];
        ys = new int[capacity];
        zs = new int[capacity];
        kinds = new EndBlock[capacity];
    }

    /** Starts a run under the block at {@code anchorY}, which was or was not placed. */
    void start(int anchorY, boolean placed) {
        n = 0;
        half = anchorY / SPLIT_Y;
        anchorPlaced = placed;
    }

    void add(int x, int y, int z, EndBlock b) {
        if (y / SPLIT_Y != half || n == kinds.length) return;
        xs[n] = x;
        ys[n] = y;
        zs[n] = z;
        kinds[n++] = b;
    }

    void flush() {
        boolean clear = anchorPlaced;
        for (int i = 0; i < n && clear; i++)
            if (inside(sink, xs[i], ys[i], zs[i]) && !sink.isAir(xs[i], ys[i], zs[i])) clear = false;
        if (clear) for (int i = 0; i < n; i++)
            if (inside(sink, xs[i], ys[i], zs[i])) sink.place(xs[i], ys[i], zs[i], kinds[i]);
        n = 0;
    }

    static boolean inside(BlockSink s, int x, int y, int z) {
        return x >= s.originX() && x < s.originX() + s.width()
            && z >= s.originZ()
            && z < s.originZ() + s.width()
            && y >= s.minY()
            && y < s.maxY();
    }
}
