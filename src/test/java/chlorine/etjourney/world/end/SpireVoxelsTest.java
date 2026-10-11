package chlorine.etjourney.world.end;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.Spire;
import chlorine.etjourney.world.end.feature.StructureProbe;
import chlorine.etjourney.world.end.region.RegionPicker;
import chlorine.etjourney.world.end.region.Styles;

/** Spire needles come out of the block pass in SPIRES regions, the same in both halves of a column. */
class SpireVoxelsTest {

    private static final TerrainSampler SAMPLER = new TerrainSampler(91L, new RegionPicker(Styles.all()));

    @Test
    void needlesAreDrawnInSpiresChunks() {
        StructureProbe probe = SAMPLER.structureProbe();
        int drawn = 0, spires = 0, kept = 0;
        for (int[] c : CloudSheetsTest.chunksOf("SPIRES", 12)) {
            ChunkPlan plan = new ChunkPlan(SAMPLER, c[0], c[1], (a, b) -> Collections.emptyList());
            List<Spire> near = Spire.KIND.near(91L, c[0] * 16 + 8, c[1] * 16 + 8, 8, probe);
            spires += near.size();
            for (Spire s : near) if (s.outsideHoles(91L, plan.holes()) != null) kept++;
            MemorySink sink = CloudSheetsTest.draw(c, 0, 256, (a, b) -> Collections.emptyList());
            for (int x = c[0] * 16; x < c[0] * 16 + 16; x++) {
                for (int z = c[1] * 16; z < c[1] * 16 + 16; z++) {
                    for (int y = 0; y < 256; y++) {
                        if (sink.get(x, y, z) == null) continue;
                        for (Spire s : near) if (s.density(x + 0.5, y + 0.5, z + 0.5) >= 0) {
                            drawn++;
                            break;
                        }
                    }
                }
            }
        }
        System.out.println("spires " + spires + " kept " + kept + " drawn " + drawn);
        assertTrue(spires >= 24, "only " + spires + " spires near 12 SPIRES chunks");
        assertTrue(drawn > 300, "only " + drawn + " needle blocks drawn");
    }

    @Test
    void halvesAgree() {
        CloudVoxelsTest.assertHalvesAgree("SPIRES");
    }
}
