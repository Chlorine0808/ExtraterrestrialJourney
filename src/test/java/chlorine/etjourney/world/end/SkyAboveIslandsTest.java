package chlorine.etjourney.world.end;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.ArcPaths;
import chlorine.etjourney.world.end.feature.ZoneIslands;
import chlorine.etjourney.world.end.region.RegionPicker;
import chlorine.etjourney.world.end.region.Styles;
import chlorine.etjourney.world.end.reserve.Area;
import chlorine.etjourney.world.end.reserve.Reservations;

/** Over a reserved island, floating features keep only what lies above the reserved height. */
class SkyAboveIslandsTest {

    private static final long SEED = 77L;
    private static final TerrainSampler SAMPLER = new TerrainSampler(SEED, new RegionPicker(Styles.all()));

    /** A chunk under a large island reservation. */
    private static ChunkPlan underIsland(int cx, int cz) {
        List<Area> island = Collections.singletonList(new Area("hee", cx * 16 + 8, cz * 16 + 8, 400));
        return new ChunkPlan(SAMPLER, cx, cz, (a, b) -> island);
    }

    @Test
    void arcsPassHighOverIslands() {
        int high = 0;
        for (int i = 0; i < 400 && high == 0; i++) {
            int cx = 80 + i * 7 % 300, cz = -150 + i * 13 % 300;
            if (SAMPLER.styleWeight("ARCS", cx * 16 + 8, cz * 16 + 8) < 0.5) continue;
            ArcPaths.Segments arcs = underIsland(cx, cz).arcs();
            if (arcs.isEmpty()) continue;
            assertTrue(arcs.minY() >= Reservations.CLEAR_Y - 1, "an arc dips into the island at " + arcs.minY());
            high++;
        }
        assertTrue(high > 0, "no arc passes over any island");
    }

    @Test
    void highZoneIslandsStayOverIslands() {
        ZoneIslands.Island high = null;
        for (int cx = -60; cx < 60 && high == null; cx++) {
            for (int cz = -60; cz < 60 && high == null; cz++) {
                ZoneIslands.Island z = ZoneIslands.inCell(SEED, cx, cz, SAMPLER.landProbe());
                if (z != null && z.y - z.down >= Reservations.CLEAR_Y) high = z;
            }
        }
        if (high == null) return;
        List<Area> island = Collections.singletonList(new Area("hee", high.x, high.z, 128));
        assertTrue(
            ZoneIslands.withoutReserved(Collections.singletonList(high), island)
                .size() == 1,
            "a zone island high above the island was removed");
    }
}
