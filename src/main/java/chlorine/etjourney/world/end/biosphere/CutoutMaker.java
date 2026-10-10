package chlorine.etjourney.world.end.biosphere;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import chlorine.etjourney.core.config.ETJConfig;
import chlorine.etjourney.core.util.ModLog;
import chlorine.etjourney.world.end.feature.Bedrock;
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

    /**
     * The cut-out of a ball, from its own kind of world or else the other one; null when neither could make it,
     * so the ball is left out rather than left empty.
     */
    static Cutout make(long seed, Biosphere b) {
        for (BiosphereSource.Kind kind : BiosphereSource.order(BiosphereSource.kind(seed, b))) {
            WorldServer w = SampleWorlds.world(kind);
            if (w == null) continue;
            try {
                return cut(seed, b, kind, w);
            } catch (Throwable e) {
                // Another mod's generator failing in the sample dimension; try the other world.
                SampleWorlds.rethrowFatal(e);
                // Vanilla population sets this for its duration; an exception part way would leave sand falling
                // instantly everywhere.
                BlockFalling.fallInstantly = false;
                ModLog.LOG
                    .warn("Biosphere at {},{} could not be cut from {}", (int) b.centreX, (int) b.centreZ, kind, e);
            }
        }
        return null;
    }

    private static Cutout cut(long seed, Biosphere b, BiosphereSource.Kind kind, WorldServer w) {
        long start = System.nanoTime();
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
            int height = w.getActualHeight();
            Cutout cut = new Cutout(b);
            int y0 = Math.max(0, (int) Math.floor(b.minY())), y1 = Math.min(255, (int) Math.ceil(b.maxY()));
            for (int x = (int) Math.floor(b.minX()); x <= (int) Math.ceil(b.maxX()); x++) {
                for (int z = (int) Math.floor(b.minZ()); z <= (int) Math.ceil(b.maxZ()); z++) {
                    int sx = p[0] + x - cx, sz = p[1] + z - cz;
                    Bedrock.Column column = column(w, sx, sz);
                    for (int y = y0; y <= y1; y++) {
                        if (b.part(x, y, z) != Biosphere.Part.INSIDE) continue;
                        int sy = y + dy;
                        if (sy < 0 || sy > 255) continue;
                        Block block = w.getBlock(sx, sy, sz);
                        if (block == Blocks.bedrock) {
                            // Bedrock gives way to the rock beside it, or to air.
                            sy = Bedrock.standIn(column, sy, height);
                            if (sy < 0) continue;
                            block = w.getBlock(sx, sy, sz);
                        }
                        int meta = w.getBlockMetadata(sx, sy, sz);
                        if (block.hasTileEntity(meta) && !keepTile(w, cut, x, y, z, sx, sy, sz)) continue;
                        cut.set(x, y, z, Block.getIdFromBlock(block) << 4 | meta);
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

    /**
     * Keeps the tile entity data of the sample block at (sx, sy, sz) for the ball's (x, y, z); false when there
     * is none to keep, and the block is then left out: a mod's tile entity built empty may fail as it ticks.
     */
    private static boolean keepTile(WorldServer w, Cutout cut, int x, int y, int z, int sx, int sy, int sz) {
        try {
            TileEntity tile = w.getTileEntity(sx, sy, sz);
            if (tile == null) return false;
            NBTTagCompound tag = new NBTTagCompound();
            tile.writeToNBT(tag);
            cut.putTile(x, y, z, tag);
            return true;
        } catch (Throwable t) {
            SampleWorlds.rethrowFatal(t);
            return false;
        }
    }

    /** Column (x, z) of the sample, for finding what stands in for its bedrock. */
    private static Bedrock.Column column(WorldServer w, int x, int z) {
        return new Bedrock.Column() {

            @Override
            public boolean bedrock(int y) {
                return y >= 0 && y <= 255 && w.getBlock(x, y, z) == Blocks.bedrock;
            }

            @Override
            public boolean solid(int y) {
                if (y < 0 || y > 255) return false;
                Block block = w.getBlock(x, y, z);
                return block != Blocks.bedrock && block.getMaterial()
                    .isSolid();
            }
        };
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
        // Someone who reached the sample dimension keeps its chunks; they unload the usual way after.
        if (!w.playerEntities.isEmpty()) return;
        List<Chunk> chunks = new ArrayList<>(w.theChunkProviderServer.loadedChunks);
        for (Chunk chunk : chunks) {
            // They stop ticking now; the world forgets them on its next entity update.
            for (List<?> list : chunk.entityLists) w.loadedEntityList.removeAll(list);
            w.loadedTileEntityList.removeAll(chunk.chunkTileEntityMap.values());
            chunk.onChunkUnload();
            w.theChunkProviderServer.loadedChunks.remove(chunk);
            w.theChunkProviderServer.loadedChunkHashMap
                .remove(ChunkCoordIntPair.chunkXZ2Int(chunk.xPosition, chunk.zPosition));
        }
        // An idle world skips its entity updates, which is where unloaded entities and tile entities leave its
        // lists; without this they would stay there for the rest of the run.
        w.resetUpdateEntityTick();
    }
}
