package chlorine.etjourney.world.end.modifier;

import java.util.List;

import chlorine.etjourney.world.end.reserve.Area;

/** The chunk being generated: seed, position and the reserved areas around it. */
public final class ChunkArea {

    public final long seed;
    public final int chunkX, chunkZ;
    public final List<Area> reserved;

    public ChunkArea(long seed, int chunkX, int chunkZ, List<Area> reserved) {
        this.seed = seed;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.reserved = reserved;
    }

    public int originX() {
        return chunkX * 16;
    }

    public int originZ() {
        return chunkZ * 16;
    }
}
