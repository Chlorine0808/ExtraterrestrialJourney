package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** Each structure forms where its style is, stays inside the world and its footprint, and is solid in its body. */
class StructuresTest {

    /** Every style everywhere, flat land with ground at Y 70. */
    private static final StructureProbe LAND = new StructureProbe() {

        @Override
        public double weight(String style, double x, double z) {
            return 1;
        }

        @Override
        public double land(double x, double z) {
            return 60;
        }

        @Override
        public double ground(double x, double z) {
            return 70;
        }
    };

    private static <T extends Structure> T first(Structure.Kind<T> kind, long seed) {
        for (int i = 0; i < 400; i++) {
            T s = kind.inCell(seed, 10 + i % 20, i / 20, LAND);
            if (s != null) return s;
        }
        return null;
    }

    private static void inside(Structure s, double footprint) {
        assertTrue(s.maxY() <= 250 && s.minY() >= -1, "height " + s.minY() + ".." + s.maxY());
        assertTrue(s.footprint <= footprint + 1e-9, "footprint " + s.footprint);
    }

    @Test
    void mushroomsHaveASolidStalkAndCap() {
        Structures.Mushroom m = first(Structures.MUSHROOMS, 301L);
        assertNotNull(m);
        inside(m, 46);
        assertTrue(m.density(m.centreX, m.base + 5, m.centreZ) > 0);
        assertTrue(m.density(m.centreX + m.cap * 0.7, m.capY - 1, m.centreZ) > 0);
        assertTrue(m.density(m.centreX + m.cap * 0.7, m.base + 5, m.centreZ) < 0);
    }

    @Test
    void ringsAreSolidOnTheirCircleAndHollowInTheMiddle() {
        Structures.Ring r = first(Structures.RINGS, 302L);
        assertNotNull(r);
        inside(r, 280);
        assertTrue(r.density(r.centreX, r.y, r.centreZ) < 0);
        double px = r.centreX + r.u[0] * r.radius, py = r.y + r.u[1] * r.radius, pz = r.centreZ + r.u[2] * r.radius;
        assertTrue(r.density(px, py, pz) > 0);
    }

    @Test
    void archesSpanBetweenTwoFeet() {
        Structures.Arch a = first(Structures.ARCHES, 303L);
        assertNotNull(a);
        inside(a, 52);
        assertTrue(a.maxY() > 70 + 10, "arch top " + a.maxY());
    }

    @Test
    void spiralTowersHaveAPillarAndAWindingRamp() {
        Structures.SpiralTower t = first(Structures.SPIRAL_TOWERS, 304L);
        assertNotNull(t);
        inside(t, 24);
        assertTrue(t.density(t.centreX, (t.base + t.top) / 2, t.centreZ) > 0);
        assertTrue(t.top - t.base >= 100);
    }

    @Test
    void hollowPillarsAreHollow() {
        Structures.HollowPillar p = first(Structures.HOLLOW_PILLARS, 305L);
        assertNotNull(p);
        inside(p, 36);
        assertTrue(p.density(p.centreX, (p.base + p.top) / 2, p.centreZ) < 0);
        int solid = 0;
        for (int a = 0; a < 36; a++) {
            double angle = Math.toRadians(a * 10);
            if (p.density(
                p.centreX + Math.cos(angle) * p.radius,
                (p.base + p.top) / 2,
                p.centreZ + Math.sin(angle) * p.radius) > 0) solid++;
        }
        assertTrue(solid > 18, "wall mostly solid, " + solid + "/36");
    }

    @Test
    void structuresOnlyFormWhereTheirStyleIs() {
        StructureProbe none = new StructureProbe() {

            @Override
            public double weight(String style, double x, double z) {
                return 0;
            }

            @Override
            public double land(double x, double z) {
                return 60;
            }

            @Override
            public double ground(double x, double z) {
                return 70;
            }
        };
        for (int i = 0; i < 100; i++) {
            assertTrue(Structures.MUSHROOMS.inCell(306L, i, 1, none) == null);
            assertTrue(Structures.RINGS.inCell(306L, i, 3, none) == null);
        }
    }

    /** Flat ground at the given height everywhere, every style everywhere. */
    private static StructureProbe groundAt(double height) {
        return new StructureProbe() {

            @Override
            public double weight(String style, double x, double z) {
                return 1;
            }

            @Override
            public double land(double x, double z) {
                return 60;
            }

            @Override
            public double ground(double x, double z) {
                return height;
            }
        };
    }

    @Test
    void structuresOnCeilingHighGroundAreSkippedOrStayInTheWorld() {
        StructureProbe high = groundAt(250);
        for (Structure.Kind<? extends Structure> kind : Structures.kinds()) {
            for (int i = 0; i < 300; i++) {
                Structure s = kind.inCell(307L, 20 + i % 20, i / 20, high);
                if (s == null) continue;
                assertTrue(s.minY() < s.maxY() && s.maxY() <= 250, kind.style + " " + s.minY() + ".." + s.maxY());
            }
        }
    }

    @Test
    void ringsStayInsideTheWorld() {
        int seen = 0;
        for (int i = 0; i < 2000; i++) {
            Structures.Ring r = Structures.RINGS.inCell(308L, 5 + i % 40, i / 40, LAND);
            if (r == null) continue;
            seen++;
            assertTrue(r.maxY() <= 250 && r.minY() >= 0, "ring " + r.minY() + ".." + r.maxY());
        }
        assertTrue(seen > 50);
    }

    @Test
    void nothingFormsNearTheCentralIsland() {
        for (Structure.Kind<? extends Structure> kind : Structures.kinds()) {
            for (int cx = -12; cx <= 12; cx++) {
                for (int cz = -12; cz <= 12; cz++) {
                    Structure s = kind.inCell(309L, cx, cz, LAND);
                    if (s != null) assertTrue(Math.hypot(s.centreX, s.centreZ) >= Structures.MIN_RADIUS, kind.style);
                }
            }
        }
    }

    @Test
    void mushroomCapsSitOnTheirStalks() {
        int checked = 0;
        for (int i = 0; i < 400; i++) {
            Structures.Mushroom m = Structures.MUSHROOMS.inCell(310L, 20 + i % 20, i / 20, LAND);
            if (m == null) continue;
            checked++;
            double top = m.capY + m.dome;
            for (double y = m.base + 1; y < top - 1; y += 0.5) {
                assertTrue(m.density(m.centreX, y, m.centreZ) > 0, "gap at " + (y - m.base) + " above the base");
            }
        }
        assertTrue(checked > 20);
    }

    @Test
    void ringsSitAtTheirRegionCentreWhereTheStyleIsEven() {
        for (int i = 0; i < 40; i++) {
            Structures.Ring r = Structures.RINGS.inCell(311L, 4 + i, 6, LAND);
            if (r == null) continue;
            double[] centre = LAND.regionCentre(4 + i, 6);
            assertTrue(Math.hypot(r.centreX - centre[0], r.centreZ - centre[1]) < 1e-9, "ring off its region centre");
        }
    }

    @Test
    void ringletsAreManySmallAndInsideTheWorld() {
        int found = 0;
        for (int cx = 20; cx < 40; cx++) {
            for (int cz = 0; cz < 20; cz++) {
                Structures.Ring r = Structures.RINGLETS.inCell(312L, cx, cz, LAND);
                if (r == null) continue;
                found++;
                assertTrue(r.minY() >= 0 && r.maxY() <= 250, "ringlet " + r.minY() + ".." + r.maxY());
                assertTrue(r.footprint <= 40, "footprint " + r.footprint);
                assertTrue(r.tube < 4, "tube " + r.tube);
            }
        }
        assertTrue(found > 120, "only " + found + " ringlets in 400 cells");
    }

    @Test
    void archesAreDense() {
        // At least about one arch per 90 x 90 blocks of ARCHES land.
        Set<String> seen = new HashSet<>();
        for (int x = 20000; x < 22000; x += 100) {
            for (int z = 0; z < 2000; z += 100) {
                for (Structures.Arch a : Structures.ARCHES.near(313L, x, z, 50, LAND))
                    seen.add(a.centreX + "," + a.centreZ);
            }
        }
        assertTrue(seen.size() >= 480, "only " + seen.size() + " arches in 2000 x 2000 blocks");
    }

    @Test
    void archesTryOtherAnglesToFindGround() {
        // Land only in a band 70 blocks wide along x: an arch must lie along the band to stand on it.
        StructureProbe band = new StructureProbe() {

            @Override
            public double weight(String style, double x, double z) {
                return 1;
            }

            @Override
            public double land(double x, double z) {
                return Math.abs(z - 35) < 35 ? 60 : -50;
            }

            @Override
            public double ground(double x, double z) {
                return land(x, z) > 0 ? 70 : -1000;
            }
        };
        int found = 0;
        for (int cx = 300; cx < 500; cx++) {
            if (Structures.ARCHES.inCell(314L, cx, 0, band) != null) found++;
        }
        assertTrue(found >= 130, "only " + found + " of 200 cells along the band have an arch");
    }

    @Test
    void crossesStandInTheGroundOrFloat() {
        int stuck = 0, floating = 0;
        for (int cx = 20; cx < 60; cx++) {
            for (int cz = 0; cz < 20; cz++) {
                Structures.Cross c = Structures.CROSSES.inCell(315L, cx, cz, LAND);
                if (c == null) continue;
                inside(c, 75);
                double[] stem = c.point(c.height * 0.5, 0), arm = c.point(c.armAt, c.armHalf * 0.8);
                assertTrue(c.density(stem[0], stem[1], stem[2]) > 0, "hollow stem");
                assertTrue(c.density(arm[0], arm[1], arm[2]) > 0, "hollow arm");
                if (c.feet().length > 0) {
                    stuck++;
                    assertTrue(c.point(0, 0)[1] < 70, "a stuck cross does not reach into the ground");
                } else {
                    floating++;
                }
            }
        }
        assertTrue(stuck > 10 && floating > 5, stuck + " stuck, " + floating + " floating");
    }

    @Test
    void crossesOverTheVoidFloat() {
        StructureProbe voidOnly = new StructureProbe() {

            @Override
            public double weight(String style, double x, double z) {
                return 1;
            }

            @Override
            public double land(double x, double z) {
                return -50;
            }

            @Override
            public double ground(double x, double z) {
                return -1000;
            }
        };
        int seen = 0;
        for (int cx = 20; cx < 60; cx++) {
            Structures.Cross c = Structures.CROSSES.inCell(316L, cx, 3, voidOnly);
            if (c == null) continue;
            seen++;
            assertEquals(0, c.feet().length);
            inside(c, 75);
        }
        assertTrue(seen > 10);
    }
}
