package chlorine.etjourney.world.end.feature.cloud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** One small puff per cell on a gently waving sheet, sometimes with a second tier above it. */
class MackerelTest {

    @Test
    void onePuffInEveryCell() {
        assertEquals(1600, Probes.count(Mackerel.KIND, 551L, Probes.VOID));
    }

    @Test
    void puffsSitOnTheWavingSheet() {
        int second = 0;
        int c0 = Probes.cell0(Mackerel.KIND);
        for (int i = 0; i < 400; i++) {
            Mackerel m = Mackerel.KIND.inCell(552L, c0 + i % 20, c0 + i / 20, Probes.VOID);
            assertNotNull(m);
            assertTrue(Math.abs(m.centreY - Mackerel.LEVEL) <= 20, "height " + m.centreY);
            if (m.tiers == 2) second++;
        }
        assertTrue(second > 40 && second < 200, second + " second tiers of 400");
    }

    @Test
    void noPuffInsideHighGround() {
        assertEquals(0, Probes.count(Mackerel.KIND, 553L, Probes.of(1, 60, 150, 120)));
    }

    @Test
    void theFootprintCoversTheFarthestLobe() {
        // A lobe of size 7 at the corner of its 4-block offset reaches 4 * sqrt(2) + 7 from the centre.
        Mackerel m = new Mackerel(0, 0, 120, 1, new double[] { 4, 120, 4, 7 });
        assertTrue(m.footprint >= 4 * Math.sqrt(2) + 7, "footprint " + m.footprint);
    }
}
