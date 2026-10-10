package chlorine.etjourney.world.end;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.Structures;
import chlorine.etjourney.world.end.modifier.EndBlock;
import chlorine.etjourney.world.end.modifier.Shape;
import chlorine.etjourney.world.end.region.RegionPicker;
import chlorine.etjourney.world.end.region.Styles;

/** Mushrooms are drawn block by block, so their round caps keep their shape, the same in both halves. */
class MushroomVoxelsTest {

    private static final long SEED = 77L;
    private static final TerrainSampler SAMPLER = new TerrainSampler(SEED, new RegionPicker(Styles.all()));

    private static int[] mushroomChunk() {
        for (int i = 0; i < 300; i++) {
            for (int j = 0; j < 300; j++) {
                int cx = 100 + i * 3, cz = -450 + j * 3;
                double x = cx * 16 + 8, z = cz * 16 + 8;
                if (SAMPLER.styleWeight("MUSHROOMS", x, z) < 0.75) continue;
                if (!Structures.MUSHROOMS.near(SEED, x, z, 8, SAMPLER.structureProbe())
                    .isEmpty()) return new int[] { cx, cz };
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
    void mushroomsAreNoDensityShapes() {
        int[] c = mushroomChunk();
        assertNotNull(c, "no mushroom found in a MUSHROOMS region");
        for (Shape s : new ChunkPlan(SAMPLER, c[0], c[1], (a, b) -> Collections.emptyList()).shapes()) {
            assertFalse(s instanceof Structures.Mushroom, "a mushroom in the density shapes");
        }
    }

    @Test
    void mushroomVoxelsAreDrawn() {
        int[] c = mushroomChunk();
        assertNotNull(c);
        MemorySink sink = draw(c, 0, 256);
        List<Structures.Mushroom> mushrooms = Structures.MUSHROOMS
            .near(SEED, c[0] * 16 + 8, c[1] * 16 + 8, 16, SAMPLER.structureProbe());
        int inside = 0;
        for (int x = c[0] * 16; x < c[0] * 16 + 16; x++) {
            for (int z = c[1] * 16; z < c[1] * 16 + 16; z++) {
                for (int y = 0; y < 256; y++) {
                    for (Structures.Mushroom m : mushrooms) {
                        if (m.density(x + 0.5, y + 0.5, z + 0.5) < 0) continue;
                        inside++;
                        assertTrue(sink.get(x, y, z) != null, "hole in a mushroom at " + x + "," + y + "," + z);
                    }
                }
            }
        }
        assertTrue(inside > 0, "the mushroom does not reach the chunk's blocks");
    }

    @Test
    void bothHalvesDrawTheSameMushrooms() {
        int[] c = mushroomChunk();
        assertNotNull(c);
        MemorySink whole = draw(c, 0, 256), low = draw(c, 0, 128), high = draw(c, 128, 256);
        for (int x = c[0] * 16; x < c[0] * 16 + 16; x++) {
            for (int z = c[1] * 16; z < c[1] * 16 + 16; z++) {
                for (int y = 0; y < 256; y++) {
                    EndBlock half = y < 128 ? low.get(x, y, z) : high.get(x, y, z);
                    assertEquals(whole.get(x, y, z), half, "at " + x + "," + y + "," + z);
                }
            }
        }
    }
}
