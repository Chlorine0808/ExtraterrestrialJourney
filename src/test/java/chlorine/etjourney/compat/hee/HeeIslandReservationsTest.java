package chlorine.etjourney.compat.hee;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.reserve.Area;

class HeeIslandReservationsTest {

    @Test
    void islandIsCentredOnItsFootprintEastAndSouthOfTheStart() {
        List<Area> found = HeeIslandReservations.scan(10, 20, 0, (cx, cz) -> cx == 10 && cz == 20);
        assertEquals(1, found.size());
        assertEquals(10 * 16 + 104, found.get(0).x, 1e-9);
        assertEquals(20 * 16 + 104, found.get(0).z, 1e-9);
        assertEquals(128, found.get(0).radius, 1e-9);
    }

    @Test
    void startsFarEnoughWestOrNorthToReachTheChunkAreFound() {
        // The scan reaches 208 / 16 + 1 = 14 chunks back, one past the island's 13-chunk width.
        assertEquals(
            1,
            HeeIslandReservations.scan(0, 0, 0, (cx, cz) -> cx == -14 && cz == -14)
                .size());
        assertTrue(
            HeeIslandReservations.scan(0, 0, 0, (cx, cz) -> cx == -15 || cz == -15)
                .isEmpty());
    }

    @Test
    void startsEastOrSouthOfTheRadiusAreNotScanned() {
        assertTrue(
            HeeIslandReservations.scan(0, 0, 2, (cx, cz) -> cx == 3 || cz == 3)
                .isEmpty());
        assertEquals(
            1,
            HeeIslandReservations.scan(0, 0, 2, (cx, cz) -> cx == 2 && cz == 2)
                .size());
    }
}
