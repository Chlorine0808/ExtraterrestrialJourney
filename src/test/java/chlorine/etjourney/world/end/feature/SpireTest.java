package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Spires: clusters of thin, sharp needles standing on the land of SPIRES regions, drawn block by block. */
class SpireTest {

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
        return (int) Math.ceil(Structures.MIN_RADIUS / Spire.KIND.cell) + 1;
    }

    @Test
    void thinSharpNeedlesStandOnTheGround() {
        int c0 = cell0(), seen = 0;
        for (int i = 0; i < 400; i++) {
            Spire s = Spire.KIND.inCell(870L, c0 + i % 20, c0 + i / 20, probe(1, 70));
            if (s == null) continue;
            seen++;
            assertEquals("SPIRES", Spire.KIND.style);
            for (Spire.Needle n : s.needles) {
                assertTrue(n.radius <= 3, "thin needle " + n.radius);
                assertTrue(n.y0 < 70, "rooted in the ground at " + n.y0);
                assertTrue(s.body(n.x0, 71, n.z0) > 0, "solid just above the ground");
                // Within the top fifth the needle is at most one block across.
                double tip = n.y0 + (n.y1 - n.y0) * 0.8;
                double[] axis = n.axisAt(tip);
                assertTrue(n.density(axis[0] + 1, tip, axis[2]) < 0, "sharp tip");
                assertTrue(n.density(n.x1, n.y1 + 1, n.z1) < 0, "nothing above the tip");
                assertTrue(
                    Math.hypot(n.x0 - s.centreX, n.z0 - s.centreZ) + n.radius < s.footprint,
                    "foot in footprint");
                assertTrue(Math.hypot(n.x1 - s.centreX, n.z1 - s.centreZ) < s.footprint, "tip in footprint");
                assertTrue(n.y1 < s.maxY() && n.y0 > s.minY());
            }
        }
        // Most cells hold a cluster, so the needles come thick.
        assertTrue(seen > 250, "seen " + seen);
    }

    @Test
    void noNeedlesOverTheVoidOrOutsideSpires() {
        int c0 = cell0();
        for (int i = 0; i < 200; i++) {
            assertNull(Spire.KIND.inCell(871L, c0 + i % 20, c0 + i / 20, probe(1, -1000)));
            assertNull(Spire.KIND.inCell(872L, c0 + i % 20, c0 + i / 20, probe(0, 70)));
        }
    }
}
