package chlorine.etjourney.world.end.feature.cloud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Thin streaks hang from the underside and fray away further down. */
class VirgaTest {

    @Test
    void streaksFrayDownwards() {
        Virga v = Probes.first(Virga.KIND, 561L, Probes.LAND);
        assertNotNull(v);
        int upper = 0, lower = 0;
        double quarter = v.longest / 4;
        for (int x = (int) Math.floor(v.centreX - 7); x <= v.centreX + 7; x++) {
            for (int z = (int) Math.floor(v.centreZ - 7); z <= v.centreZ + 7; z++) {
                for (int y = (int) Math.floor(v.top - v.longest); y <= v.top; y++) {
                    if (v.density(x + 0.5, y + 0.5, z + 0.5) < 0) continue;
                    if (y > v.top - quarter) upper++;
                    else if (y < v.top - 3 * quarter) lower++;
                }
                assertTrue(v.density(x + 0.5, v.top + 1.5, z + 0.5) < 0, "above the top");
            }
        }
        assertTrue(upper > lower, upper + " upper against " + lower + " lower");
    }

    @Test
    void noStreakWithoutAnUnderside() {
        assertEquals(0, Probes.count(Virga.KIND, 562L, Probes.VOID));
    }
}
