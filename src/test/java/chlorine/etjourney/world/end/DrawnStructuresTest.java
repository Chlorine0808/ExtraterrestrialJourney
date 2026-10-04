package chlorine.etjourney.world.end;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.Structure;
import chlorine.etjourney.world.end.feature.Structures;
import chlorine.etjourney.world.end.modifier.EndBlock;
import chlorine.etjourney.world.end.region.RegionPicker;
import chlorine.etjourney.world.end.region.Styles;

/** Kinds drawn block by block land in real chunks whole, in their block, and both halves of a column agree. */
class DrawnStructuresTest {

    private static final long SEED = 77L;
    private static final TerrainSampler SAMPLER = new TerrainSampler(SEED, new RegionPicker(Styles.all()));

    private static int[] chunkWith(Structure.Kind<? extends Structure> kind) {
        for (int i = 0; i < 400; i++) {
            for (int j = 0; j < 400; j++) {
                int cx = 100 + i * 3, cz = -600 + j * 3;
                double x = cx * 16 + 8, z = cz * 16 + 8;
                if (SAMPLER.styleWeight(kind.style, x, z) < 0.75) continue;
                for (Structure s : kind.near(SEED, x, z, 12, SAMPLER.structureProbe())) {
                    if (reaches(s, cx, cz)) return new int[] { cx, cz };
                }
            }
        }
        return null;
    }

    /** Whether the structure's body, not just its footprint, reaches the chunk. */
    private static boolean reaches(Structure s, int cx, int cz) {
        for (int x = cx * 16; x < cx * 16 + 16; x += 2) {
            for (int z = cz * 16; z < cz * 16 + 16; z += 2) {
                for (int y = (int) Math.max(0, s.minY()); y < Math.min(256, s.maxY()); y += 2) {
                    if (s.density(x + 0.5, y + 0.5, z + 0.5) >= 0) return true;
                }
            }
        }
        return false;
    }

    private static MemorySink draw(int[] c, int minY, int maxY) {
        MemorySink sink = new MemorySink(c[0], c[1], minY, maxY);
        new ChunkPlan(SAMPLER, c[0], c[1], (a, b) -> Collections.emptyList()).blocks(sink);
        return sink;
    }

    private static void check(Structure.Kind<? extends Structure> kind, EndBlock block) {
        int[] c = chunkWith(kind);
        assertNotNull(c, "no " + kind.style + " found");
        MemorySink whole = draw(c, 0, 256), low = draw(c, 0, 128), high = draw(c, 128, 256);
        List<? extends Structure> near = kind.near(SEED, c[0] * 16 + 8, c[1] * 16 + 8, 16, SAMPLER.structureProbe());
        int inside = 0;
        for (int x = c[0] * 16; x < c[0] * 16 + 16; x++) {
            for (int z = c[1] * 16; z < c[1] * 16 + 16; z++) {
                for (int y = 0; y < 256; y++) {
                    EndBlock half = y < 128 ? low.get(x, y, z) : high.get(x, y, z);
                    assertEquals(whole.get(x, y, z), half, "halves differ at " + x + "," + y + "," + z);
                    for (Structure s : near) {
                        if (s.density(x + 0.5, y + 0.5, z + 0.5) < 0) continue;
                        inside++;
                        assertTrue(
                            whole.get(x, y, z) != null,
                            "hole in " + kind.style + " at " + x + "," + y + "," + z);
                    }
                }
            }
        }
        assertTrue(inside > 0, kind.style + " does not reach the chunk's blocks");
        boolean drawnInBlock = false;
        for (int x = c[0] * 16; x < c[0] * 16 + 16 && !drawnInBlock; x++) {
            for (int z = c[1] * 16; z < c[1] * 16 + 16 && !drawnInBlock; z++) {
                for (int y = 0; y < 256; y++) {
                    if (whole.get(x, y, z) == block) {
                        drawnInBlock = true;
                        break;
                    }
                }
            }
        }
        assertTrue(drawnInBlock, kind.style + " not drawn in " + block);
    }

    @Test
    void crossesAreDrawn() {
        check(Structures.CROSSES, EndBlock.STONE);
    }

    @Test
    void hangingChainsAreDrawnInChainBlocks() {
        check(Structures.HANGING_CHAINS, EndBlock.CHAIN);
    }

    @Test
    void skyChainsAreDrawnInChainBlocks() {
        check(Structures.SKY_CHAINS, EndBlock.CHAIN);
    }
}
