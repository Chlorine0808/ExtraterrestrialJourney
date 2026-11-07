package chlorine.etjourney.world.end.building;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.building.Placement;
import chlorine.etjourney.world.building.Rotation;
import chlorine.etjourney.world.building.Template;
import chlorine.etjourney.world.building.TemplateParser;
import chlorine.etjourney.world.end.feature.StructureProbe;

/** Laying End buildings out one per cell, the same from every window. */
class BuildingKindTest {

    private static final long SEED = 1234;
    private static final StructureProbe FLAT = BuildingSiteTest.ground((x, z) -> 64);

    /** 10 wide along x, 60 deep along z, so a quarter turn swaps its long side. */
    private static Template narrow() throws Exception {
        StringBuilder rows = new StringBuilder("[");
        for (int z = 0; z < 60; z++) rows.append(z == 0 ? "" : ",")
            .append("\"AAAAAAAAAA\"");
        rows.append(']');
        String json = "[{\"layers\":[{\"rows\":" + rows + "}],\"mappings\":{\"A\":\"m:a\"}}]";
        return TemplateParser.parse("narrow", new StringReader(json), true);
    }

    private static boolean same(Placement a, Placement b) {
        return a.anchorX == b.anchorX && a.anchorY == b.anchorY
            && a.anchorZ == b.anchorZ
            && a.rotation == b.rotation
            && a.salt == b.salt;
    }

    /** The placements of every cell with a building in 30 x 30 cells from (20, 20). */
    private static List<Placement> sample(BuildingKind kind, Template t, StructureProbe probe) {
        List<Placement> all = new ArrayList<>();
        for (int cx = 20; cx < 50; cx++) for (int cz = 20; cz < 50; cz++) {
            Placement p = kind.compute(SEED, cx, cz, t, probe, probe);
            if (p != null) all.add(p);
        }
        return all;
    }

    @Test
    void sameCellSameResultFromAnyWindow() throws Exception {
        Template t = narrow();
        BuildingKind kind = BuildingKind.builder("k", "narrow")
            .cell(96)
            .chance(1)
            .build();
        for (Placement p : sample(kind, t, FLAT).subList(0, 5)) {
            for (int wx = Math.floorDiv(p.minX() - 8, 16) * 16 + 8; wx <= p.maxX(); wx += 16)
                for (int wz = Math.floorDiv(p.minZ() - 8, 16) * 16 + 8; wz <= p.maxZ(); wz += 16) {
                    boolean found = false;
                    for (Placement q : kind.near(SEED, wx, wz, t, FLAT, FLAT)) found |= same(p, q);
                    assertTrue(found);
                }
        }
    }

    @Test
    void findsRotatedBuildingFromEveryTouchingWindow() throws Exception {
        Template t = narrow();
        BuildingKind kind = BuildingKind.builder("k", "narrow")
            .cell(64)
            .chance(1)
            .build();
        boolean[] seen = new boolean[4];
        for (Placement p : sample(kind, t, FLAT)) {
            seen[p.rotation.quarterTurns()] = true;
            // Windows one step outside each side do not see it; windows inside do.
            for (int wx = Math.floorDiv(p.minX() - 8, 16) * 16 + 8 - 16; wx <= p.maxX() + 16; wx += 16)
                for (int wz = Math.floorDiv(p.minZ() - 8, 16) * 16 + 8 - 16; wz <= p.maxZ() + 16; wz += 16) {
                    boolean touches = wx <= p.maxX() && wx + 15 >= p.minX() && wz <= p.maxZ() && wz + 15 >= p.minZ();
                    boolean found = false;
                    for (Placement q : kind.near(SEED, wx, wz, t, FLAT, FLAT)) found |= same(p, q);
                    assertEquals(touches, found);
                }
        }
        for (Rotation r : Rotation.values()) assertTrue(seen[r.quarterTurns()], r.name());
    }

    @Test
    void nothingInsideMinRadius() throws Exception {
        BuildingKind kind = BuildingKind.builder("k", "narrow")
            .cell(64)
            .chance(1)
            .build();
        for (int cx = -10; cx < 10; cx++)
            for (int cz = -10; cz < 10; cz++) assertNull(kind.compute(SEED, cx, cz, narrow(), FLAT, FLAT));
    }

    @Test
    void styleFilter() throws Exception {
        StructureProbe noArches = new StructureProbe() {

            @Override
            public double weight(String style, double x, double z) {
                return style.equals("ARCHES") ? 0.4 : 1;
            }

            @Override
            public double land(double x, double z) {
                return 1;
            }

            @Override
            public double ground(double x, double z) {
                return 64;
            }
        };
        BuildingKind arches = BuildingKind.builder("k", "narrow")
            .cell(64)
            .chance(1)
            .styles("ARCHES")
            .build();
        assertTrue(sample(arches, narrow(), noArches).isEmpty());
        assertFalse(sample(arches, narrow(), FLAT).isEmpty());
    }

    @Test
    void floatingUsesRange() throws Exception {
        BuildingKind kind = BuildingKind.builder("k", "narrow")
            .cell(64)
            .chance(1)
            .floating(100, 150)
            .build();
        StructureProbe voidOnly = BuildingSiteTest.ground((x, z) -> -1000);
        List<Placement> all = sample(kind, narrow(), voidOnly);
        assertFalse(all.isEmpty());
        for (Placement p : all) assertTrue(p.anchorY >= 100 && p.anchorY <= 150, "" + p.anchorY);
    }

    @Test
    void chanceThinsCells() throws Exception {
        BuildingKind kind = BuildingKind.builder("k", "narrow")
            .cell(64)
            .chance(0.2)
            .build();
        int n = sample(kind, narrow(), FLAT).size();
        assertTrue(n > 900 * 0.1 && n < 900 * 0.3, "" + n);
    }

    @Test
    void standingSinksIntoGround() throws Exception {
        BuildingKind kind = BuildingKind.builder("k", "narrow")
            .cell(64)
            .chance(1)
            .build();
        for (Placement p : sample(kind, narrow(), FLAT)) assertEquals(63, p.bottomY());
    }
}
