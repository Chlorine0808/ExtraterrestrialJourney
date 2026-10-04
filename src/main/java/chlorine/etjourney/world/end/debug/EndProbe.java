package chlorine.etjourney.world.end.debug;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.init.Blocks;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import chlorine.etjourney.core.util.ModLog;
import chlorine.etjourney.world.end.EndTerrain;
import chlorine.etjourney.world.end.TerrainSampler;
import chlorine.etjourney.world.end.region.Style;

/**
 * Headless measurements of the End terrain, run at server start only when a file named etj-endscan exists in the
 * server directory: generation time, unpopulated chunks, land coverage, surface heights and style shares.
 */
public final class EndProbe {

    private static final int CX0 = 40, CX1 = 112, CZ0 = -56, CZ1 = 16;

    private EndProbe() {}

    public static void runIfRequested() {
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
}
