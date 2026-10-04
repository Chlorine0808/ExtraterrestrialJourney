package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.reserve.Area;

/** Placement rules of the cell features, checked with simple stand-in terrain probes. */
class PlacementTest {

    /** Flat land everywhere: ground 60, bottom 20, land 80. */
    private static final Lakes.Probe FLAT = new Lakes.Probe() {

        @Override
        public boolean allowed(double x, double z) {
            return true;
        }

        @Override
        public double land(double x, double z) {
            return 80;
        }

        @Override
        public double ground(double x, double z, boolean mountains) {
            return 60;
        }

        @Override
        public double bottom(double x, double z) {
            return 20;
        }
    };

    @Test
    void lakeShapeHasBasinAndFlatRing() {
        List<Lakes.Lake> lakes = Lakes.near(101L, 5000, 5000, 400, FLAT);
        assertFalse(lakes.isEmpty());
        Lakes.Lake lake = lakes.get(0);
        assertTrue(Lakes.shape(lakes, 60, lake.x, lake.z) <= lake.waterLevel);
        assertEquals(lake.waterLevel + 2, Lakes.shape(lakes, 60, lake.x + lake.radius + 5, lake.z), 1e-9);
        assertTrue(lake.waterLevel - lake.depth >= 20 + Lakes.MIN_FLOOR);
    }

    @Test
    void reservedAreasDropWholeLakesAndIslands() {
        List<Lakes.Lake> lakes = Lakes.near(102L, 5000, 5000, 400, FLAT);
        Lakes.Lake lake = lakes.get(0);
        List<Area> area = Collections.singletonList(new Area("hee", lake.x + 150, lake.z, 128));
        assertFalse(
            Lakes.withoutReserved(lakes, area)
                .contains(lake));
    }

    @Test
    void holesCutFullyNearTheirCentreAndNotFarAway() {
        Holes.Probe probe = new Holes.Probe() {

            @Override
            public boolean allowed(double x, double z) {
                return true;
            }

            @Override
            public double land(double x, double z) {
                return 80;
            }

            @Override
            public double top(double x, double z) {
                return 60;
            }

            @Override
            public List<Lakes.Lake> lakesNear(double x, double z, double range) {
                return Collections.emptyList();
            }
        };
        List<Holes.Hole> holes = Holes.near(103L, 6000, 6000, 200, probe);
        assertFalse(holes.isEmpty());
        Holes.Hole hole = holes.get(0);
        double best = 0;
        for (int dx = -20; dx <= 20; dx++) {
            for (int dz = -20; dz <= 20; dz++)
                best = Math.max(best, Holes.cutFraction(103L, holes, hole.x + dx, hole.z + dz));
        }
        assertEquals(1, best, 1e-9);
        for (int i = 0; i < 400; i++) {
            double c = Holes.cutFraction(103L, holes, hole.x + i * 0.37, hole.z - i * 0.21);
            assertTrue(c >= 0 && c <= 1);
        }
    }

    @Test
    void zoneIslandsAvoidLandAndKeepOneZone() {
        ZoneIslands.LandProbe noLand = (x, z) -> -100;
        ZoneIslands.LandProbe allLand = (x, z) -> 50;
        assertNull(ZoneIslands.inCell(104L, 20, 20, allLand) == null ? null : "island on land");
        int found = 0;
        for (int cx = 10; cx < 30; cx++) {
            ZoneIslands.Island island = ZoneIslands.inCell(105L, cx, 7, noLand);
            if (island == null) continue;
            found++;
            assertTrue(island.y + island.up * 1.2 < 256);
            assertEquals(ZoneIslands.zoneOf(105L, island), ZoneIslands.zoneOf(105L, island));
        }
        assertTrue(found > 0);
    }

    @Test
    void shoalsNeverFormInsideAReservedIsland() {
        Shoals.Probe probe = new Shoals.Probe() {

            @Override
            public boolean dense(double x, double z) {
                return true;
            }

            @Override
            public List<ZoneIslands.Island> zoneIslandsNear(double x, double z) {
                return Collections.emptyList();
            }
        };
        for (Shoals.School school : Shoals.near(106L, 8000, 8000, 300, probe)) {
            List<Area> onTop = Collections.singletonList(new Area("hee", school.x + 100, school.z, 128));
            assertFalse(Shoals.forms(school, onTop));
            for (int[] b : Shoals.blocks(school)) {
                assertTrue(Math.hypot(b[0] - school.x, b[2] - school.z) < Shoals.EXTENT + 60);
            }
        }
    }

    @Test
    void isletSpansShrinkTowardsTheRim() {
        Islets.Probe probe = new Islets.Probe() {

            @Override
            public double weight(double x, double z) {
                return 1;
            }

            @Override
            public List<ZoneIslands.Island> zoneIslandsNear(double x, double z) {
                return Collections.emptyList();
            }
        };
        Islets.Islet islet = Islets.near(107L, 9000, 9000, 100, probe)
            .get(0);
        int[] centre = Islets.span(islet, 0), edge = Islets.span(islet, islet.radius * 0.9);
        assertTrue(centre[1] - centre[0] > edge[1] - edge[0]);
        assertNull(Islets.span(islet, islet.radius));
    }

    @Test
    void arcPointsStayInTheWorldAndTubesAreSolidOnTheirPath() {
        List<ArcPaths.Path> paths = ArcPaths.pathsNear(108L, 7000, 7000, 400, (x, z) -> 1);
        assertFalse(paths.isEmpty());
        ArcPaths.Segments all = ArcPaths.segmentsNear(paths, -1e9, 1e9, -1e9, 1e9, (x, y, z) -> true);
        for (ArcPaths.Path path : paths) {
            double[] start = path.start();
            assertTrue(start[1] >= 6 && start[1] <= 250);
            assertTrue(ArcPaths.density(all, start[0], start[1], start[2]) >= path.tube - 1e-6);
        }
    }

    @Test
    void arcSegmentsNearAChunkGiveTheSameDensityThere() {
        List<ArcPaths.Path> paths = ArcPaths.pathsNear(108L, 7000, 7000, 400, (x, z) -> 1);
        ArcPaths.Segments all = ArcPaths.segmentsNear(paths, -1e9, 1e9, -1e9, 1e9, (x, y, z) -> true);
        double[] start = paths.get(0)
            .start();
        int ox = (int) start[0] - 8, oz = (int) start[2] - 8;
        ArcPaths.Segments local = all.within(ox - 8, ox + 24, oz - 8, oz + 24);
        assertTrue(local.count < all.count, "nothing was left out");
        for (int x = ox - 8; x <= ox + 24; x += 4) {
            for (int z = oz - 8; z <= oz + 24; z += 4) {
                for (int y = 0; y < 256; y += 8) {
                    assertEquals(ArcPaths.density(all, x, y, z), ArcPaths.density(local, x, y, z), 0);
                }
            }
        }
    }
}
