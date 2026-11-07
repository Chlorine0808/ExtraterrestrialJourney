package chlorine.etjourney.world.end;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.Structures;
import chlorine.etjourney.world.end.modifier.EndBlock;
import chlorine.etjourney.world.end.modifier.Shape;
import chlorine.etjourney.world.end.region.RegionPicker;
import chlorine.etjourney.world.end.region.Styles;

/** Large rings are drawn block by block, so their tubes are not cut into the density grid's steps. */
class RingVoxelsTest {

    private static final long SEED = 77L;
    private static final TerrainSampler SAMPLER = new TerrainSampler(SEED, new RegionPicker(Styles.all()));

    private static Structures.Ring ring() {
        for (int i = 0; i < 400; i++) {
            Structures.Ring r = Structures.RINGS.inCell(SEED, i % 20 - 10, i / 20 - 10, SAMPLER.structureProbe());
            if (r != null) return r;
        }
        return null;
    }

    /** The chunk where the ring's centre line passes at angle 0. */
    private static int[] ringChunk() {
        Structures.Ring r = ring();
        if (r == null) return null;
        double[] p = r.pointOnRing(0);
        return new int[] { (int) Math.floor(p[0]) >> 4, (int) Math.floor(p[2]) >> 4 };
    }

    private static MemorySink draw(int[] c, int minY, int maxY) {
        MemorySink sink = new MemorySink(c[0], c[1], minY, maxY);
        new ChunkPlan(SAMPLER, c[0], c[1], (a, b) -> Collections.emptyList()).blocks(sink);
        return sink;
    }

    @Test
    void ringsAreNoDensityShapes() {
        int[] c = ringChunk();
        assertNotNull(c, "no ring found");
        for (Shape s : new ChunkPlan(SAMPLER, c[0], c[1], (a, b) -> Collections.emptyList()).shapes()) {
            assertFalse(s instanceof Structures.Ring, "a ring in the density shapes");
        }
    }

    @Test
    void ringVoxelsFollowTheExactTube() {
        int[] c = ringChunk();
        assertNotNull(c);
        Structures.Ring r = ring();
        MemorySink sink = draw(c, 0, 256);
        int inside = 0;
        for (int x = c[0] * 16; x < c[0] * 16 + 16; x++) {
            for (int z = c[1] * 16; z < c[1] * 16 + 16; z++) {
                for (int y = 0; y < 256; y++) {
                    if (r.density(x + 0.5, y + 0.5, z + 0.5) < 0) continue;
                    inside++;
                    assertTrue(sink.get(x, y, z) != null, "hole in a ring at " + x + "," + y + "," + z);
                }
            }
        }
        assertTrue(inside > 0, "the ring does not reach the chunk's blocks");
    }

    @Test
    void bothHalvesDrawTheSameRing() {
        int[] c = ringChunk();
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
