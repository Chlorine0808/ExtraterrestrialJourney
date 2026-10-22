package chlorine.etjourney.world.end;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.Biosphere;
import chlorine.etjourney.world.end.modifier.EndBlock;
import chlorine.etjourney.world.end.region.RegionPicker;
import chlorine.etjourney.world.end.region.Styles;

/** Biospheres are drawn block by block: a glass shell around cleared air. */
class BiosphereVoxelsTest {

    private static final long SEED = 77L;
    private static final TerrainSampler SAMPLER = new TerrainSampler(SEED, new RegionPicker(Styles.all()));

    /** A chunk through the middle of a biosphere, so it holds shell, air and floor. */
    private static int[] biosphereChunk() {
        for (int i = 0; i < 400; i++) {
            for (int j = 0; j < 400; j++) {
                int cx = 100 + i * 3, cz = -600 + j * 3;
                double x = cx * 16 + 8, z = cz * 16 + 8;
                if (SAMPLER.styleWeight("BIOSPHERES", x, z) < 0.75) continue;
                for (Biosphere b : Biosphere.KIND.near(SEED, x, z, 8, SAMPLER.structureProbe())) {
                    int bx = (int) Math.floor(b.centreX) >> 4, bz = (int) Math.floor(b.centreZ) >> 4;
                    return new int[] { bx, bz };
                }
            }
        }
        return null;
    }

    private static MemorySink draw(int[] c, int minY, int maxY) {
        MemorySink sink = new MemorySink(c[0], c[1], minY, maxY);
        new ChunkPlan(SAMPLER, c[0], c[1], (a, b) -> Collections.emptyList()).blocks(sink);
        return sink;
    }

    @Test
    void everyPartIsDrawn() {
        int[] c = biosphereChunk();
        assertNotNull(c, "no biosphere found in a BIOSPHERES region");
        MemorySink sink = draw(c, 0, 256);
        List<Biosphere> balls = Biosphere.KIND.near(SEED, c[0] * 16 + 8, c[1] * 16 + 8, 12, SAMPLER.structureProbe());
        int glass = 0, inside = 0;
        for (int x = c[0] * 16; x < c[0] * 16 + 16; x++) {
            for (int z = c[1] * 16; z < c[1] * 16 + 16; z++) {
                for (int y = 0; y < 256; y++) {
                    for (Biosphere b : balls) {
                        Biosphere.Part part = b.part(x, y, z);
                        String at = part + " at " + x + "," + y + "," + z;
                        if (part == Biosphere.Part.GLASS) {
                            glass++;
                            assertEquals(EndBlock.GLASS, sink.get(x, y, z), at);
                        } else if (part == Biosphere.Part.INSIDE) {
                            inside++;
                            assertNull(sink.get(x, y, z), at);
                        }
                    }
                }
            }
        }
        assertTrue(glass > 0 && inside > 0, "glass " + glass + ", inside " + inside);
    }

    @Test
    void bothHalvesDrawTheSameBiosphere() {
        int[] c = biosphereChunk();
        assertNotNull(c);
        MemorySink whole = draw(c, 0, 256), low = draw(c, 0, 128), high = draw(c, 128, 256);
        for (int x = c[0] * 16; x < c[0] * 16 + 16; x++) {
            for (int z = c[1] * 16; z < c[1] * 16 + 16; z++) {
                for (int y = 0; y < 256; y++) {
                    MemorySink half = y < 128 ? low : high;
                    String at = "at " + x + "," + y + "," + z;
                    assertEquals(whole.get(x, y, z), half.get(x, y, z), at);
                }
            }
        }
    }
}
