package chlorine.etjourney.world.end;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.StructureProbe;
import chlorine.etjourney.world.end.region.RegionPicker;
import chlorine.etjourney.world.end.region.Styles;

/**
 * A SHATTERED crack in the Lucentwoods where the 2026-11-15 in-game check found trees standing on nothing: the land
 * before the styles is positive there, but the crack drops the column's own land below zero and no rock is laid. The
 * probes must answer void, so nothing is placed on the crack.
 */
class ProbeCrackTest {

    private static final long SEED = -3346200354689737098L;
    private static final double X = 448.5, Z = -3210.5;

    @Test
    void probesSeeTheCrackAsVoid() {
        TerrainSampler sampler = new TerrainSampler(SEED, new RegionPicker(Styles.all()));
        assertTrue(sampler.land(X, Z) > 0, "the spot must lie inside the continent before the styles");
        assertTrue(sampler.bareColumn(X, Z, true).land <= 0, "the spot must lie in a crack");

        StructureProbe bare = sampler.structureProbe();
        StructureProbe standing = sampler.standingProbe();
        assertTrue(bare.ground(X, Z) <= -100, "bare ground " + bare.ground(X, Z));
        assertTrue(bare.surface(X, Z) <= -100, "bare surface " + bare.surface(X, Z));
        assertTrue(bare.underside(X, Z) <= -100, "bare underside " + bare.underside(X, Z));
        assertTrue(standing.ground(X, Z) <= -100, "standing ground " + standing.ground(X, Z));
        assertTrue(standing.surface(X, Z) <= -100, "standing surface " + standing.surface(X, Z));
    }
}
