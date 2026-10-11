package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Giant tools in CROSSES regions: a tool sprite blown up to 2-5 blocks a pixel and as thick as it is wide, driven into
 * the ground at a lean or floating at any angle.
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
        Set<ToolSprite> seen = EnumSet.noneOf(ToolSprite.class);
        int c0 = cell0();
        for (int i = 0; i < 1600; i++) {
            Tool t = Tool.KIND.inCell(872L, c0 + i % 40, c0 + i / 40, probe(1, 70));
            if (t != null) seen.add(t.sprite);
        }
        assertEquals(EnumSet.allOf(ToolSprite.class), seen);
    }

    @Test
    void theShapeFollowsTheSprite() {
        int c0 = cell0(), checked = 0;
        for (int i = 0; i < 400; i++) {
            Tool t = Tool.KIND.inCell(873L, c0 + i % 20, c0 + i / 20, probe(1, 70));
            if (t == null) continue;
            checked++;
            for (int col = 0; col < 16; col++) {
                for (int row = 0; row < 16; row++) {
                    double[] p = t.pixel(col, row, 0);
                    double d = t.body(p[0], p[1], p[2]);
                    assertTrue(t.sprite.filled(col, row) ? d > 0 : d < 0, t.sprite + " pixel " + col + "," + row);
                    if (!t.sprite.filled(col, row)) continue;
                    double half = t.sprite.thickness(col, row) * t.scale / 2;
                    for (double w : new double[] { -1, 1 }) {
                        double[] in = t.pixel(col, row, w * (half - 0.1)), out = t.pixel(col, row, w * (half + 0.1));
                        assertTrue(t.body(in[0], in[1], in[2]) > 0, t.sprite + " too thin at " + col + "," + row);
                        assertTrue(t.body(out[0], out[1], out[2]) < 0, t.sprite + " too thick at " + col + "," + row);
                        double[] q = t.pixel(col, row, w * t.depth / 2);
                        assertTrue(Math.hypot(q[0] - t.centreX, q[2] - t.centreZ) <= t.footprint, "footprint");
                        assertTrue(q[1] >= t.minY() && q[1] <= t.maxY(), "height");
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
            int[] planted = t.sprite.planted();
            double low = Double.MAX_VALUE, high = -Double.MAX_VALUE;
            for (int col = 0; col < 16; col++) {
                for (int row = 0; row < 16; row++) {
                    if (!t.sprite.filled(col, row)) continue;
                    double y = t.pixel(col, row, 0)[1];
                    low = Math.min(low, y);
                    high = Math.max(high, y);
                }
            }
            if (t.feet().length > 0) {
                stuck++;
                assertTrue(t.pixel(planted[0], planted[1], 0)[1] < 70, t.sprite + " planted end above the ground");
                assertTrue(high > 70 + 20, t.sprite + " barely sticks out");
            } else {
                floating++;
                assertTrue(low > 70 + 4, t.sprite + " floating tool touches the ground");
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
