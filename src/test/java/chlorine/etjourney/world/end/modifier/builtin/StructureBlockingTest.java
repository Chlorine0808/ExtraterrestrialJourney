package chlorine.etjourney.world.end.modifier.builtin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.Structure;
import chlorine.etjourney.world.end.feature.StructureProbe;
import chlorine.etjourney.world.end.feature.Structures;
import chlorine.etjourney.world.end.reserve.Area;
import chlorine.etjourney.world.end.reserve.Reservations;

/** Grounded structures keep off land that a reserved island has faded away; floating ones may hang over it. */
class StructureBlockingTest {

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

    /** An island east of the structure, as close as touches() allows, where its fade still lowers the ground. */
    private static List<Area> islandBeside(Structure s) {
        double d = s.footprint + 128 * 1.2 + 2;
        return Collections.singletonList(new Area("hee", s.centreX + d, s.centreZ, 128));
    }

    @Test
    void groundedStructuresKeepOffFadedLand() {
        Structure mushroom = null;
        for (int i = 0; mushroom == null && i < 400; i++) mushroom = Structures.MUSHROOMS.inCell(401L, 30 + i, 5, LAND);
        assertNotNull(mushroom);
        List<Area> island = islandBeside(mushroom);
        assertFalse(Reservations.touches(island, mushroom.centreX, mushroom.centreZ, mushroom.footprint));
        assertTrue(Reservations.suppression(island, mushroom.centreX, mushroom.centreZ) > 0.1);
        assertTrue(StructureModifiers.blocked(island, mushroom), "mushroom stands on faded land");
    }

    @Test
    void ringsMayHangBesideAnIsland() {
        Structure ring = null;
        for (int i = 0; ring == null && i < 40; i++) ring = Structures.RINGS.inCell(402L, 4 + i, 3, LAND);
        assertNotNull(ring);
        assertFalse(StructureModifiers.blocked(islandBeside(ring), ring));
    }
}
