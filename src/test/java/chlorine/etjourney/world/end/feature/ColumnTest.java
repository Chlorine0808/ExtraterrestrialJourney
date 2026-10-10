package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Solid columns among the hollow pillars: thin tall ones and short stubby ones, drawn block by block. */
class ColumnTest {

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

    @Test
    void thinTallOnesAndShortStubbyOnes() {
        int c0 = (int) Math.ceil(Structures.MIN_RADIUS / Column.KIND.cell) + 1, thin = 0, stubby = 0;
        assertEquals("HOLLOW_PILLARS", Column.KIND.style);
        for (int i = 0; i < 1600; i++) {
            Column c = Column.KIND.inCell(860L, c0 + i % 40, c0 + i / 40, LAND);
            if (c == null) continue;
            double height = c.top - c.base;
            if (c.radius <= 3.5) {
                thin++;
                assertTrue(height >= 30, "thin column only " + height + " tall");
            } else {
                stubby++;
                assertTrue(height <= 20, "stubby column " + height + " tall");
            }
            double mid = (c.base + c.top) / 2;
            assertTrue(c.body(c.centreX, mid, c.centreZ) > 0, "solid core");
            assertTrue(c.body(c.centreX + c.radius + 1, mid, c.centreZ) < 0, "round side");
            assertTrue(c.body(c.centreX, c.top + 1, c.centreZ) < 0, "flat top");
            assertTrue(c.radius + 1 <= c.footprint && c.minY() >= 0 && c.maxY() <= 250);
        }
        assertTrue(thin > 20 && stubby > 20, thin + " thin, " + stubby + " stubby");
    }
}
