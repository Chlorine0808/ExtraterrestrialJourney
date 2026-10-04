package chlorine.etjourney.world.end.reserve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

class ReservationsTest {

    private static final List<Area> ONE = Collections.singletonList(new Area("hee", 2000, 0, 128));

    @Test
    void suppressionIsFullInsideAndZeroBeyondTheFade() {
        assertEquals(1, Reservations.suppression(ONE, 2000, 0), 1e-9);
        assertEquals(0, Reservations.suppression(ONE, 2000 + 128 * 1.15 + Reservations.FADE + 31, 0), 1e-9);
    }

    @Test
    void suppressionChangesSmoothlyAlongARay() {
        double prev = Reservations.suppression(ONE, 2000, 0);
        for (int d = 1; d < 400; d++) {
            double s = Reservations.suppression(ONE, 2000 + d * 0.7, d * 0.7);
            assertTrue(Math.abs(s - prev) < 0.1, "jump at " + d);
            prev = s;
        }
    }

    @Test
    void footprintCoversTheIslandSquare() {
        assertTrue(Reservations.insideFootprint(ONE, 2000 + 128 * 1.2 - 1, 0, 0));
        assertFalse(Reservations.insideFootprint(ONE, 2000 + 128 * 1.2 + 1, 0, 0));
    }
}
