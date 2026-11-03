package chlorine.etjourney.world.end.feature.plant;

import java.util.Map;

import chlorine.etjourney.world.end.modifier.BlockSink;
import chlorine.etjourney.world.end.modifier.EndBlock;

/** A sink over a shared map; wider than 16 on purpose. */
final class MapSink implements BlockSink {

    final Map<Long, EndBlock> cells;
    final int ox, oz, size, y0, y1;

    MapSink(Map<Long, EndBlock> cells, int ox, int oz, int size, int y0, int y1) {
        this.cells = cells;
        this.ox = ox;
        this.oz = oz;
        this.size = size;
        this.y0 = y0;
        this.y1 = y1;
    }

    static long key(int x, int y, int z) {
        return ((long) x & 0xFFFFFFL) << 40 | ((long) y & 0xFFFL) << 24 | ((long) z & 0xFFFFFFL);
    }

    @Override
    public int originX() {
        return ox;
    }

    @Override
    public int originZ() {
        return oz;
    }

    @Override
    public int width() {
        return size;
    }

    @Override
    public int minY() {
        return y0;
    }

    @Override
    public int maxY() {
        return y1;
    }

    private boolean inside(int x, int y, int z) {
        return x >= ox && x < ox + size && z >= oz && z < oz + size && y >= y0 && y < y1;
    }

    @Override
    public boolean isAir(int x, int y, int z) {
        return !cells.containsKey(key(x, y, z));
    }

    @Override
    public boolean is(int x, int y, int z, EndBlock kind) {
        return cells.get(key(x, y, z)) == kind;
    }

    @Override
    public void place(int x, int y, int z, EndBlock block) {
        if (inside(x, y, z)) cells.putIfAbsent(key(x, y, z), block);
    }

    @Override
    public void set(int x, int y, int z, EndBlock block) {
        if (inside(x, y, z)) cells.put(key(x, y, z), block);
    }

    @Override
    public void clear(int x, int y, int z) {
        cells.remove(key(x, y, z));
    }
}
