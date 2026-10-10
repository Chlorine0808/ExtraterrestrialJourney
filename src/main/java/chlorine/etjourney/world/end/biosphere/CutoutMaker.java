package chlorine.etjourney.world.end.biosphere;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import chlorine.etjourney.core.config.ETJConfig;
import chlorine.etjourney.core.util.ModLog;
import chlorine.etjourney.world.end.feature.Biosphere;
import chlorine.etjourney.world.end.feature.BiosphereSource;
import chlorine.etjourney.world.end.feature.Cutout;
import chlorine.etjourney.world.end.feature.FloorFinder;

/** Generates a biosphere's sample in its hidden dimension, cuts the ball out of it and throws the chunks away. */
final class CutoutMaker {

    /** Highest floor a cave cut-out looks at, how far under the surface it must lie, and the lowest floor. */
    private static final int CAVE_TOP = 56, CAVE_DEPTH = 12, LOWEST_FLOOR = 6;
    /** Floor height when the Nether has none. */
    private static final int NETHER_FALLBACK = 64;

    private CutoutMaker() {}

    static Cutout make(long seed, Biosphere b) {
        long start = System.nanoTime();
        BiosphereSource.Kind kind = BiosphereSource.kind(seed, b);
        WorldServer w = SampleWorlds.world(kind);
        int[] p = BiosphereSource.point(seed, b, kind, (x, z) -> SampleWorlds.isWater(w, x, z));
        try {
            // A chunk is decorated by its own populate and its west and north neighbours'; each populate needs
            // the chunks east and south of it. So load one chunk beyond the ball on every side.
            int reach = (int) Math.ceil(b.footprint);
            for (int i = ((p[0] - reach) >> 4) - 1; i <= ((p[0] + reach) >> 4) + 1; i++) {
                for (int j = ((p[1] - reach) >> 4) - 1; j <= ((p[1] + reach) >> 4) + 1; j++) {
                    w.theChunkProviderServer.loadChunk(i, j);
                }
            }
            int cx = (int) Math.floor(b.centreX), cz = (int) Math.floor(b.centreZ);
            int dy = ground(w, kind, p, b) - b.floorTop(cx, cz);
            Cutout cut = new Cutout(b);
            int y0 = Math.max(0, (int) Math.floor(b.minY())), y1 = Math.min(255, (int) Math.ceil(b.maxY()));
            for (int x = (int) Math.floor(b.minX()); x <= (int) Math.ceil(b.maxX()); x++) {
                for (int z = (int) Math.floor(b.minZ()); z <= (int) Math.ceil(b.maxZ()); z++) {
                    for (int y = y0; y <= y1; y++) {
                        if (b.part(x, y, z) != Biosphere.Part.INSIDE) continue;
                        int sx = p[0] + x - cx, sy = y + dy, sz = p[1] + z - cz;
                        if (sy < 0 || sy > 255) continue;
                        Block block = w.getBlock(sx, sy, sz);
                        cut.set(x, y, z, Block.getIdFromBlock(block) << 4 | w.getBlockMetadata(sx, sy, sz));
                    }
                }
            }
            if (ETJConfig.debugLogging) ModLog.LOG.info(
                "Biosphere {},{} cut from {} {},{} in {} ms",
                cx,
                cz,
                kind,
                p[0],
                p[1],
                (System.nanoTime() - start) / 1_000_000);
            return cut;
        } finally {
            discard(w);
        }
    }

    /** The floor height of the sample, which the ball's floor level is aligned to. */
    private static int ground(WorldServer w, BiosphereSource.Kind kind, int[] p, Biosphere b) {
        int surface = w.getTopSolidOrLiquidBlock(p[0], p[1]) - 1;
        if (kind == BiosphereSource.Kind.SURFACE) return surface;
        FloorFinder.Blocks blocks = new FloorFinder.Blocks() {

            @Override
            public boolean ground(int x, int y, int z) {
                Material m = w.getBlock(x, y, z)
                    .getMaterial();
                return m.isSolid() || m.isLiquid();
            }

            @Override
            public boolean air(int x, int y, int z) {
                return w.isAirBlock(x, y, z);
            }
        };
        int high = kind == BiosphereSource.Kind.CAVE ? Math.min(CAVE_TOP, surface - CAVE_DEPTH)
            : w.getActualHeight() - 8;
        int floor = FloorFinder.broadest(blocks, p[0], p[1], b.radius, LOWEST_FLOOR, high);
        if (floor >= 0) return floor;
        return kind == BiosphereSource.Kind.CAVE ? surface : NETHER_FALLBACK;
    }

    /**
     * Throws every chunk of the sample dimension away unsaved. Unloading the usual way would save them, and
     * levelSaving stops unloading altogether in 1.7.10.
     */
    private static void discard(WorldServer w) {
        List<Chunk> chunks = new ArrayList<>(w.theChunkProviderServer.loadedChunks);
        for (Chunk chunk : chunks) {
            // An idle world stops updating its entities, which is where unloaded ones leave its lists; take the
            // chunk's entities and tile entities out here instead.
            for (List<?> list : chunk.entityLists) {
                for (Object o : list) {
                    Entity e = (Entity) o;
                    w.loadedEntityList.remove(e);
                    w.onEntityRemoved(e);
                }
            }
            w.loadedTileEntityList.removeAll(chunk.chunkTileEntityMap.values());
            chunk.onChunkUnload();
            w.theChunkProviderServer.loadedChunks.remove(chunk);
            w.theChunkProviderServer.loadedChunkHashMap
                .remove(ChunkCoordIntPair.chunkXZ2Int(chunk.xPosition, chunk.zPosition));
        }
    }
}
