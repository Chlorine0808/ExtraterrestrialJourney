package chlorine.etjourney.world.end.modifier;

import java.util.List;

import chlorine.etjourney.world.end.reserve.Area;

/** The chunk being generated: seed, position and the reserved areas around it. */
public final class ChunkArea {

    public final long seed;
    public final int chunkX, chunkZ;
    public final List<Area> reserved;
    /** The chunk's features and per-column scales; null in tests of the chain alone. */
    public final TerrainView view;

    public ChunkArea(long seed, int chunkX, int chunkZ, List<Area> reserved, TerrainView view) {
        this.seed = seed;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.reserved = reserved;
        this.view = view;
    }

    public int originX() {
        return chunkX * 16;
    }

    public int originZ() {
        return chunkZ * 16;
    }
}
