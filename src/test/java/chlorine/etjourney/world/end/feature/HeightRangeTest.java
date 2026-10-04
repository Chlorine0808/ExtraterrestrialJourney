package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Features use the whole world height now that Y 128-255 is written. */
class HeightRangeTest {

    @Test
    void arcPathsReachAboveTheOldCeilingAndStayInTheWorld() {
        double highest = 0;
        for (ArcPaths.Path path : ArcPaths.pathsNear(201L, 7000, 7000, 1500, (x, z) -> 1)) {
            ArcPaths.Segments one = ArcPaths
                .segmentsNear(Collections.singletonList(path), -1e9, 1e9, -1e9, 1e9, (x, y, z) -> true);
            assertTrue(one.minY() >= 6 - 1e-9 && one.maxY() <= 250 + 1e-9, "path out of the world");
            highest = Math.max(highest, one.maxY());
        }
        assertTrue(highest > 140, "highest arc " + highest);
    }

    @Test
    void zoneIslandsMayFloatAboveTheOldCeiling() {
        double highest = 0;
        for (int cx = 6; cx < 60; cx++) {
            for (int cz = -20; cz < 20; cz++) {
                ZoneIslands.Island island = ZoneIslands.inCell(202L, cx, cz, (x, z) -> -100);
                if (island == null) continue;
                double top = island.y + island.up * 1.2;
                assertTrue(top < 256, "island top " + top);
                highest = Math.max(highest, top);
            }
        }
        assertTrue(highest > 140, "highest island " + highest);
    }

    @Test
    void isletsAndShoalsReachHigherThanBefore() {
        Islets.Probe isletProbe = new Islets.Probe() {

            @Override
            public double weight(double x, double z) {
                return 1;
            }

            @Override
            public List<ZoneIslands.Island> zoneIslandsNear(double x, double z) {
                return Collections.emptyList();
            }
        };
        double isletTop = 0;
        for (Islets.Islet islet : Islets.near(203L, 9000, 9000, 400, isletProbe))
            isletTop = Math.max(isletTop, islet.y);
        assertTrue(isletTop > 130 && isletTop <= 200, "highest islet " + isletTop);
        Shoals.Probe shoalProbe = new Shoals.Probe() {

            @Override
            public boolean dense(double x, double z) {
                return true;
            }

            @Override
            public List<ZoneIslands.Island> zoneIslandsNear(double x, double z) {
                return Collections.emptyList();
            }
        };
        double shoalTop = 0;
        for (Shoals.School s : Shoals.near(204L, 9000, 9000, 400, shoalProbe)) shoalTop = Math.max(shoalTop, s.y);
        assertTrue(shoalTop > 130 && shoalTop <= 200, "highest school " + shoalTop);
    }
}
