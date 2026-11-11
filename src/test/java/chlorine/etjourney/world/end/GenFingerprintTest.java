package chlorine.etjourney.world.end;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import chlorine.etjourney.world.end.modifier.EndBlock;
import chlorine.etjourney.world.end.region.RegionPicker;
import chlorine.etjourney.world.end.region.Styles;

/**
 * Hashes density and block passes over many chunks, to compare generation before and after a refactor. Runs only
 * when ETJ_FINGERPRINT names the output file.
 */
@EnabledIfEnvironmentVariable(named = "ETJ_FINGERPRINT", matches = ".+")
class GenFingerprintTest {

    @Test
    void writeFingerprint() throws IOException {
        StringBuilder out = new StringBuilder();
        for (long seed : new long[] { 77L, -4_123_456_789L, 20261111L }) {
            TerrainSampler sampler = new TerrainSampler(seed, new RegionPicker(Styles.all()));
            Random r = new Random(seed ^ 0x5DEECE66DL);
            long total = 1125899906842597L;
            for (int n = 0; n < 40; n++) {
                int cx = r.nextInt(500) - 250, cz = r.nextInt(500) - 250;
                if (Math.abs(cx) < 30 && Math.abs(cz) < 30) cx += 60;
                ChunkPlan plan = new ChunkPlan(sampler, cx, cz, (x, z) -> Collections.emptyList());
                long h = 17;
                for (double d : plan.densityField(false, true)) h = h * 31 + Double.doubleToLongBits(d);
                for (double d : plan.densityField(true, true)) h = h * 31 + Double.doubleToLongBits(d);
                MemorySink sink = new MemorySink(cx, cz, 256);
                if (TallPass.needed(plan)) {
                    int ox = cx * 16, oz = cz * 16;
                    TallPass.forEachSolid(
                        TallPass.upperField(plan),
                        (lx, y, lz) -> { if (y <= 255) sink.set(ox + lx, y, oz + lz, EndBlock.STONE); });
                }
                plan.blocks(sink);
                for (int x = cx * 16; x < cx * 16 + 16; x++) {
                    for (int z = cz * 16; z < cz * 16 + 16; z++) {
                        for (int y = 0; y < 256; y++) {
                            EndBlock b = sink.get(x, y, z);
                            h = h * 31 + (b == null ? -1 : b.ordinal());
                        }
                    }
                }
                out.append(seed)
                    .append(' ')
                    .append(cx)
                    .append(',')
                    .append(cz)
                    .append(' ')
                    .append(Long.toHexString(h))
                    .append('\n');
                total = total * 1000003 + h;
            }
            out.append("total ")
                .append(seed)
                .append(' ')
                .append(Long.toHexString(total))
                .append('\n');
        }
        Files.write(
            Paths.get(System.getenv("ETJ_FINGERPRINT")),
            out.toString()
                .getBytes(StandardCharsets.UTF_8));
    }
}
