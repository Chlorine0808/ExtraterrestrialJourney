package chlorine.etjourney.world.end;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.StructureProbe;
import chlorine.etjourney.world.end.region.RegionPicker;
import chlorine.etjourney.world.end.region.Styles;

/**
 * SHATTERED cracks in the Lucentwoods where the 2026-11-15 in-game check found trees standing on nothing. In the crack
 * the styles drop the column's land below zero; near its edge the land is positive, but the rock is interpolated from
 * grid nodes in the crack and is not laid. The standing probe must answer only where rock is laid.
 */
class ProbeCrackTest {

    private static final long SEED = -3346200354689737098L;
    private static final TerrainSampler SAMPLER = new TerrainSampler(SEED, new RegionPicker(Styles.all()));

    @Test
    void probesSeeTheCrackAsVoid() {
        double x = 448.5, z = -3210.5;
        assertTrue(SAMPLER.land(x, z) > 0, "the spot must lie inside the continent before the styles");
        assertTrue(SAMPLER.bareColumn(x, z, true).land <= 0, "the spot must lie in a crack");

        StructureProbe bare = SAMPLER.structureProbe();
        StructureProbe standing = SAMPLER.standingProbe();
        assertTrue(bare.ground(x, z) <= -100, "bare ground " + bare.ground(x, z));
        assertTrue(bare.surface(x, z) <= -100, "bare surface " + bare.surface(x, z));
        assertTrue(bare.underside(x, z) <= -100, "bare underside " + bare.underside(x, z));
        assertTrue(standing.ground(x, z) <= -100, "standing ground " + standing.ground(x, z));
        assertTrue(standing.surface(x, z) <= -100, "standing surface " + standing.surface(x, z));
    }

    @Test
    void standingProbeSeesTheBareEdgeAsVoid() {
        double x = 498.5, z = -3174.5;
        assertTrue(SAMPLER.bareColumn(x, z, true).land > 0, "the spot must lie on the crack's edge");
        StructureProbe standing = SAMPLER.standingProbe();
        assertTrue(standing.ground(x, z) <= -100, "standing ground " + standing.ground(x, z));
        assertTrue(standing.surface(x, z) <= -100, "standing surface " + standing.surface(x, z));
    }

    /** Across the chunks around the edge, every answered surface has rock under it. */
    @Test
    void standingSurfaceLiesOnLaidRock() {
        StructureProbe standing = SAMPLER.standingProbe();
        for (int cx = 30; cx <= 32; cx++) {
            for (int cz = -200; cz <= -198; cz++) {
                ChunkPlan plan = new ChunkPlan(SAMPLER, cx, cz, (a, b) -> Collections.emptyList());
                for (int x = cx * 16; x < cx * 16 + 16; x++) {
                    for (int z = cz * 16; z < cz * 16 + 16; z++) {
                        double surface = standing.surface(x + 0.5, z + 0.5);
                        if (surface <= -100) continue;
                        int y = (int) Math.floor(surface);
                        assertTrue(
                            plan.densityAt(x, y, z) > 0,
                            "no rock under the surface at " + x + " " + y + " " + z);
                    }
                }
            }
        }
    }
}
