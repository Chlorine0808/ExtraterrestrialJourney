package chlorine.etjourney.world.end;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.Holes;
import chlorine.etjourney.world.end.feature.cloud.Virga;
import chlorine.etjourney.world.end.modifier.EndBlock;
import chlorine.etjourney.world.end.modifier.builtin.FeatureModifiers;
import chlorine.etjourney.world.end.modifier.builtin.StructureModifiers;
import chlorine.etjourney.world.end.region.RegionPicker;
import chlorine.etjourney.world.end.region.Styles;

/** Virga does not hang in a sinkhole's shaft: the hole leaves no rock above the streak's root. */
class HoleVirgaTest {

    private static final long SEED = 7037748451318983361L;
    private static final TerrainSampler SAMPLER = new TerrainSampler(SEED, new RegionPicker(Styles.all()));
    private static final int CELLS = 100, WANTED = 12;

    @Test
    void noVirgaUnderAHoledThroughColumn() {
        int chunks = 0, stray = 0;
        for (int i = -CELLS; i <= CELLS && chunks < WANTED; i++) {
            for (int j = -CELLS; j <= CELLS && chunks < WANTED; j++) {
                Holes.Hole hole = Holes.inCell(SEED, i, j, SAMPLER.holeProbe());
                if (hole == null || SAMPLER.styleWeight("VIRGA", hole.x, hole.z) <= 0) continue;
                int cx = (int) Math.floor(hole.x) >> 4, cz = (int) Math.floor(hole.z) >> 4;
                if (Virga.KIND.near(SEED, cx * 16 + 8, cz * 16 + 8, 12, SAMPLER.structureProbe())
                    .isEmpty()) continue;
                chunks++;
                stray += strayVirga(cx, cz);
            }
        }
        assertTrue(chunks > 0, "no hole under virga found");
        assertEquals(0, stray, "virga blocks in holed-through columns");
    }

    /** Stone left in the chunk's columns that a sinkhole clears through, after the holes and the virga. */
    private static int strayVirga(int cx, int cz) {
        ChunkPlan plan = new ChunkPlan(SAMPLER, cx, cz, (a, b) -> Collections.emptyList());
        MemorySink sink = new MemorySink(cx, cz, 0, 128);
        double[] field = new double[DensityBuilder.SIZE_X * DensityBuilder.SIZE_Y * DensityBuilder.SIZE_Z];
        DensityBuilder.fill(plan, field, cx * 2, cz * 2, 0);
        final int ox = cx * 16, oz = cz * 16;
        TallPass.forEachSolid(field, (x, y, z) -> sink.set(ox + x, y - TallPass.BASE_Y, oz + z, EndBlock.STONE));
        FeatureModifiers.holes()
            .blocks(plan.area(), sink, 1);
        StructureModifiers.virga(866)
            .blocks(plan.area(), sink, 1);
        List<Holes.Hole> holes = plan.holes();
        int stray = 0;
        for (int x = ox; x < ox + 16; x++) {
            for (int z = oz; z < oz + 16; z++) {
                if (!FeatureModifiers.holedThrough(SEED, holes, plan, x, z)) continue;
                for (int y = 0; y < 128; y++) if (sink.get(x, y, z) == EndBlock.STONE) stray++;
            }
        }
        return stray;
    }
}
