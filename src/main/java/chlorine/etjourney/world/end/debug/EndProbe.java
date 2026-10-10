package chlorine.etjourney.world.end.debug;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import chlorine.etjourney.core.util.ModLog;
import chlorine.etjourney.world.end.EndTerrain;
import chlorine.etjourney.world.end.TerrainSampler;
import chlorine.etjourney.world.end.feature.Biosphere;
import chlorine.etjourney.world.end.feature.BiosphereSource;
import chlorine.etjourney.world.end.region.Style;

/**
 * Headless measurements of the End terrain, run at server start only when a file named etj-endscan exists in the
 * server directory: generation time, unpopulated chunks, land coverage, surface heights and style shares.
 */
public final class EndProbe {

    private static final int CX0 = 40, CX1 = 112, CZ0 = -56, CZ1 = 16;

    private EndProbe() {}

    public static void runIfRequested() {
        if (new File("etj-biospheres").exists()) {
            biospheres();
            return;
        }
        if (!new File("etj-endscan").exists()) return;
        DimensionManager.initDimension(1);
        WorldServer end = DimensionManager.getWorld(1);
        Long seed = EndTerrain.seed();
        if (end == null || seed == null) return;
        long start = System.nanoTime();
        // Load in rings around the centre, like a player arriving there.
        int ccx = (CX0 + CX1) / 2, ccz = (CZ0 + CZ1) / 2;
        for (int r = 0; r <= (CX1 - CX0) / 2; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == r)
                        end.theChunkProviderServer.loadChunk(ccx + dx, ccz + dz);
                }
            }
        }
        int unpopulated = 0;
        for (int cx = CX0 + 1; cx < CX1; cx++) {
            for (int cz = CZ0 + 1; cz < CZ1; cz++) {
                if (!end.getChunkFromChunkCoords(cx, cz).isTerrainPopulated) unpopulated++;
            }
        }
        long[] density = EndTerrain.densityStats();
        ModLog.LOG.info(
            "[probe] loaded in {} ms; interior unpopulated {}; density field avg {} us over {} fields",
            (System.nanoTime() - start) / 1_000_000,
            unpopulated,
            density[0],
            density[1]);
        surfaces(end);
        styles(EndTerrain.sampler(seed));
    }

    private static void surfaces(WorldServer end) {
        int columns = 0, solid = 0, highest = 0;
        int[] tops = new int[16];
        for (int x = (CX0 + 1) * 16; x < CX1 * 16; x += 8) {
            for (int z = (CZ0 + 1) * 16; z < CZ1 * 16; z += 8) {
                columns++;
                for (int y = 255; y > 0; y--) {
                    if (end.getBlock(x, y, z) != Blocks.air) {
                        solid++;
                        tops[y >> 4]++;
                        highest = Math.max(highest, y);
                        break;
                    }
                }
            }
        }
        StringBuilder histogram = new StringBuilder();
        for (int i = 0; i < tops.length; i++) {
            if (tops[i] > 0) histogram.append(' ')
                .append(i * 16)
                .append("+:")
                .append(Math.round(1000.0 * tops[i] / Math.max(1, solid)) / 10.0)
                .append('%');
        }
        ModLog.LOG.info(
            "[probe] land coverage {}% of {} columns; surface Y:{}; highest {}",
            Math.round(1000.0 * solid / columns) / 10.0,
            columns,
            histogram,
            highest);
    }

    private static void styles(TerrainSampler sampler) {
        Map<Style, Double> share = new LinkedHashMap<>();
        int samples = 0;
        for (int x = -8000; x <= 8000; x += 64) {
            for (int z = -8000; z <= 8000; z += 64) {
                sampler.weights(x, z)
                    .asMap()
                    .forEach((style, w) -> share.merge(style, w, Double::sum));
                samples++;
            }
        }
        StringBuilder out = new StringBuilder();
        for (Map.Entry<Style, Double> e : share.entrySet()) {
            out.append(' ')
                .append(e.getKey())
                .append(':')
                .append(Math.round(1000.0 * e.getValue() / samples) / 10.0)
                .append('%');
        }
        ModLog.LOG.info("[probe] style weights (overlays add on top):{}", out);
    }

    /** Fills biospheres of each kind and logs, per ball, the time taken and what lies inside. */
    private static void biospheres() {
        DimensionManager.initDimension(1);
        WorldServer end = DimensionManager.getWorld(1);
        Long seed = EndTerrain.seed();
        if (end == null || seed == null) return;
        TerrainSampler sampler = EndTerrain.sampler(seed);
        // How many balls of each kind to fill: SURFACE, CAVE, NETHER.
        int[] wanted = { 3, 2, 3 };
        int left = 8;
        for (int r = 8; r < 400 && left > 0; r++) {
            for (int i = -r; i <= r && left > 0; i++) {
                for (int j = -r; j <= r && left > 0; j++) {
                    if (Math.max(Math.abs(i), Math.abs(j)) != r) continue;
                    Biosphere b = Biosphere.KIND.inCell(seed, i, j, sampler.structureProbe());
                    if (b == null) continue;
                    BiosphereSource.Kind kind = BiosphereSource.kind(seed, b);
                    if (wanted[kind.ordinal()] == 0) continue;
                    wanted[kind.ordinal()]--;
                    left--;
                    long start = System.nanoTime();
                    int c0x = ((int) Math.floor(b.minX()) >> 4) - 2, c1x = ((int) Math.floor(b.maxX()) >> 4) + 2;
                    int c0z = ((int) Math.floor(b.minZ()) >> 4) - 2, c1z = ((int) Math.floor(b.maxZ()) >> 4) + 2;
                    for (int cx = c0x; cx <= c1x; cx++) {
                        for (int cz = c0z; cz <= c1z; cz++) end.theChunkProviderServer.loadChunk(cx, cz);
                    }
                    long took = System.nanoTime() - start;
                    int glass = 0, solid = 0, inside = 0;
                    Map<String, Integer> counts = new LinkedHashMap<>();
                    for (int x = (int) Math.floor(b.minX()); x <= (int) Math.ceil(b.maxX()); x++) {
                        for (int z = (int) Math.floor(b.minZ()); z <= (int) Math.ceil(b.maxZ()); z++) {
                            for (int y = Math.max(0, (int) b.minY()); y <= Math.min(255, (int) b.maxY()); y++) {
                                Biosphere.Part part = b.part(x, y, z);
                                Block k = end.getBlock(x, y, z);
                                if (part == Biosphere.Part.GLASS && k == Blocks.glass) glass++;
                                if (part != Biosphere.Part.INSIDE) continue;
                                inside++;
                                if (k == Blocks.air) continue;
                                solid++;
                                counts.merge(k.getUnlocalizedName(), 1, Integer::sum);
                            }
                        }
                    }
                    ModLog.LOG.info(
                        "[probe] biosphere {},{},{} r={} {}: {} ms, glass {}, filled {} of {} -> {}",
                        (int) b.centreX,
                        (int) b.y,
                        (int) b.centreZ,
                        (int) b.radius,
                        kind,
                        took / 1_000_000,
                        glass,
                        solid,
                        inside,
                        counts);
                }
            }
        }
        ModLog.LOG.info("[probe] biospheres done");
    }
}
