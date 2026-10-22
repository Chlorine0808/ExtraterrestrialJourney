package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Enoki: tufts of many thin stalks with small caps, splaying out from one root, in MUSHROOMS regions. */
class EnokiTest {

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
        return (int) Math.ceil(Structures.MIN_RADIUS / Enoki.KIND.cell) + 1;
    }

    @Test
    void tuftsOfThinStalksWithCaps() {
        int c0 = cell0(), seen = 0;
        for (int i = 0; i < 400; i++) {
            Enoki e = Enoki.KIND.inCell(850L, c0 + i % 20, c0 + i / 20, probe(1, 70));
            if (e == null) continue;
            seen++;
            assertEquals("MUSHROOMS", Enoki.KIND.style);
            assertTrue(e.stalks.length >= 8, "stalks " + e.stalks.length);
            for (Enoki.Stalk st : e.stalks) {
                double mx = (st.x0 + st.x1) / 2, my = (st.y0 + st.y1) / 2, mz = (st.z0 + st.z1) / 2;
                assertTrue(e.body(mx, my, mz) > 0, "stalk middle");
                assertTrue(st.radius <= 1.5, "thin stalk " + st.radius);
                assertTrue(e.body(st.x1, st.y1 + st.capHeight * 0.5, st.z1) > 0, "cap");
                assertTrue(Math.hypot(st.x1 - e.centreX, st.z1 - e.centreZ) + st.capRadius < e.footprint, "footprint");
                assertTrue(st.y1 + st.capHeight < e.maxY(), "top");
            }
            assertTrue(e.minY() >= 0 && e.maxY() <= 250);
        }
        assertTrue(seen > 50, "seen " + seen);
    }

    @Test
    void tuftsFloatOverTheVoidOnARock() {
        int c0 = cell0(), seen = 0;
        for (int i = 0; i < 400; i++) {
            Enoki e = Enoki.KIND.inCell(851L, c0 + i % 20, c0 + i / 20, probe(1, -1000));
            if (e == null) continue;
            seen++;
            assertTrue(e.body(e.centreX, e.root - 1, e.centreZ) > 0, "rock under the root");
        }
        assertTrue(seen > 50);
    }

    @Test
    void noneAtWeightZero() {
        int c0 = cell0();
        for (int i = 0; i < 400; i++)
            assertEquals(null, Enoki.KIND.inCell(852L, c0 + i % 20, c0 + i / 20, probe(0, 70)));
    }
}
