package chlorine.etjourney.world.end.building;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.building.BlockRef;
import chlorine.etjourney.world.building.Placement;
import chlorine.etjourney.world.building.Template;
import chlorine.etjourney.world.building.TemplateParser;
import chlorine.etjourney.world.end.ChunkPlan;
import chlorine.etjourney.world.end.TerrainSampler;
import chlorine.etjourney.world.end.region.RegionPicker;
import chlorine.etjourney.world.end.region.Styles;

/**
 * The test box on the slope where the 2026-11-07 in-game check found it buried: sunk from the lowest of its five
 * points, its walls sat under the high side. On real terrain its roof must clear every column and its low side must
 * stand on the foundation.
 */
class StandingSiteTerrainTest {

    private static final long SEED = -5272020400791974488L;

    private static int surface(TerrainSampler sampler, int x, int z) {
        ChunkPlan plan = new ChunkPlan(sampler, x >> 4, z >> 4, (cx, cz) -> Collections.emptyList());
        for (int y = 250; y >= 0; y--) if (plan.densityAt(x, y, z) > 0) return y;
        return -1;
    }

    @Test
    void testBoxStandsOutOfTheSlope() throws Exception {
        TerrainSampler sampler = new TerrainSampler(SEED, new RegionPicker(Styles.all()));
        Template t;
        try (Reader r = new InputStreamReader(
            getClass().getClassLoader()
                .getResourceAsStream("assets/etjourney/buildings/test_box.json"),
            StandardCharsets.UTF_8)) {
            t = TemplateParser.parse("test_box", r, true);
        }
        BuildingKind kind = BuildingKind.builder("test_box", "test_box")
            .cell(192)
            .chance(0.5)
            .standing(6, 2)
            .foundation(new BlockRef("minecraft:end_stone", 0, null), 24)
            .build();
        Placement p = kind.compute(SEED, -8, 1, t, sampler.structureProbe(), sampler.standingProbe());
        assertNotNull(p);
        int highest = Integer.MIN_VALUE, lowest = Integer.MAX_VALUE;
        for (int x = p.minX(); x <= p.maxX(); x++) for (int z = p.minZ(); z <= p.maxZ(); z++) {
            int top = surface(sampler, x, z);
            highest = Math.max(highest, top);
            lowest = Math.min(lowest, top);
        }
        assertTrue(highest < p.maxY() - 1, "walls buried: ground " + highest + ", roof " + p.maxY());
        assertTrue(
            lowest < p.bottomY() - 1,
            "nothing for the foundation: ground " + lowest + ", bottom " + p.bottomY());
    }
}
