package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Giant tools in CROSSES regions: a tool shape blown up to 2-5 blocks a pixel, driven into the ground at a lean or
 * floating at any angle.
 */
class ToolTest {

    private static StructureProbe probe(double weight, double ground) {
        return new StructureProbe() {

            @Override
            public double weight(String style, double x, double z) {
                return weight;
            }

            @Override
            public double land(double x, double z) {
                return ground > -100 ? 60 : -100;
            }

            @Override
            public double ground(double x, double z) {
                return ground;
            }
        };
    }

    private static int cell0() {
        return (int) Math.ceil(Structures.MIN_RADIUS / Tool.KIND.cell) + 1;
    }

    @Test
    void formsOnlyInCrossesRegions() {
        assertEquals("CROSSES", Tool.KIND.style);
        int c0 = cell0(), formed = 0;
        for (int i = 0; i < 400; i++) {
            assertNull(Tool.KIND.inCell(870L, c0 + i % 20, c0 + i / 20, probe(0, 70)));
            if (Tool.KIND.inCell(871L, c0 + i % 20, c0 + i / 20, probe(1, 70)) != null) formed++;
        }
        assertTrue(formed > 50, "formed " + formed);
    }

    @Test
    void everyToolAppears() {
        Set<ToolShape> seen = EnumSet.noneOf(ToolShape.class);
        int c0 = cell0();
        for (int i = 0; i < 1600; i++) {
            Tool t = Tool.KIND.inCell(872L, c0 + i % 40, c0 + i / 40, probe(1, 70));
            if (t != null) seen.add(t.shape);
        }
        assertEquals(EnumSet.allOf(ToolShape.class), seen);
    }

    @Test
    void theBodyFollowsTheShape() {
        int c0 = cell0(), checked = 0;
        for (int i = 0; i < 400; i++) {
            Tool t = Tool.KIND.inCell(873L, c0 + i % 20, c0 + i / 20, probe(1, 70));
            if (t == null) continue;
            checked++;
            double[] b = t.shape.bounds();
            // Off the round numbers the parts are cut at, so no sample sits on a surface.
            for (double u = b[0] + 0.0137; u <= b[1]; u += 0.7) {
                for (double v = b[2] + 0.0137; v <= b[3]; v += 0.7) {
                    for (double w = b[4] + 0.0137; w <= b[5]; w += 0.7) {
                        double[] p = t.world(u, v, w);
                        boolean in = t.shape.inside(u, v, w);
                        assertEquals(in, t.body(p[0], p[1], p[2]) > 0, t.shape + " at " + u + "," + v + "," + w);
                        if (!in) continue;
                        assertTrue(Math.hypot(p[0] - t.centreX, p[2] - t.centreZ) <= t.footprint, "footprint");
                        assertTrue(p[1] >= t.minY() && p[1] <= t.maxY(), "height");
                    }
                }
            }
            assertTrue(t.minY() >= 0 && t.maxY() <= 250);
        }
        assertTrue(checked > 20);
    }

    @Test
    void groundedToolsAreDrivenInByTheirPlantedEnd() {
        int c0 = cell0(), stuck = 0, floating = 0;
        for (int i = 0; i < 1600; i++) {
            Tool t = Tool.KIND.inCell(874L, c0 + i % 40, c0 + i / 40, probe(1, 70));
            if (t == null) continue;
            double[] planted = t.shape.planted(), b = t.shape.bounds();
            double low = Double.MAX_VALUE, high = -Double.MAX_VALUE;
            for (double u = b[0]; u <= b[1]; u += 0.5) {
                for (double v = b[2]; v <= b[3]; v += 0.5) {
                    for (double w = b[4]; w <= b[5]; w += 0.5) {
                        if (!t.shape.inside(u, v, w)) continue;
                        double y = t.world(u, v, w)[1];
                        low = Math.min(low, y);
                        high = Math.max(high, y);
                    }
                }
            }
            if (t.feet().length > 0) {
                stuck++;
                assertTrue(t.world(planted[0], planted[1], 0)[1] < 70, t.shape + " planted end above the ground");
                assertTrue(high > 70 + 20, t.shape + " barely sticks out");
            } else {
                floating++;
                assertTrue(low > 70 + 4, t.shape + " floating tool touches the ground");
            }
        }
        assertTrue(stuck > 20 && floating > 10, stuck + " stuck, " + floating + " floating");
    }

    @Test
    void overTheVoidToolsFloat() {
        int c0 = cell0(), seen = 0;
        for (int i = 0; i < 400; i++) {
            Tool t = Tool.KIND.inCell(875L, c0 + i % 20, c0 + i / 20, probe(1, -1000));
            if (t == null) continue;
            seen++;
            assertEquals(0, t.feet().length);
        }
        assertTrue(seen > 20);
    }
}
