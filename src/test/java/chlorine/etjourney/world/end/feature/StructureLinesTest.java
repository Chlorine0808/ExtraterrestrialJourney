package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.cloud.Anvil;
import chlorine.etjourney.world.end.modifier.Shape;

/** A structure's vertical lines are solid exactly where its density is not negative, block by block. */
class StructureLinesTest {

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

        @Override
        public double underside(double x, double z) {
            return 50;
        }
    };

    /** Blocks where the line and the density disagree, checking every stride-th column at block centres. */
    static int disagreements(Shape s, int stride) {
        int bad = 0;
        for (int x = (int) Math.floor(s.minX()) - 1; x <= Math.ceil(s.maxX()) + 1; x += stride) {
            for (int z = (int) Math.floor(s.minZ()) - 1; z <= Math.ceil(s.maxZ()) + 1; z += stride) {
                Shape.Line line = s.line(x + 0.5, z + 0.5);
                for (int y = (int) Math.floor(s.minY()) - 2; y <= Math.ceil(s.maxY()) + 2; y++) {
                    double cy = y + 0.5;
                    boolean byLine = cy >= line.minY() && cy <= line.maxY() && line.solid(cy);
                    if (byLine != s.density(x + 0.5, cy, z + 0.5) >= 0) bad++;
                }
            }
        }
        return bad;
    }

    private static <T extends Structure> int checkKind(Structure.Kind<T> kind, long seed, int wanted, int stride) {
        int seen = 0;
        for (int i = 0; i < 4000 && seen < wanted; i++) {
            T s = kind.inCell(seed, 10 + i % 60, i / 60, LAND);
            if (s == null) continue;
            seen++;
            assertEquals(0, disagreements(s, stride), kind.style + " at " + s.centreX + "," + s.centreZ);
        }
        return seen;
    }

    @Test
    void mushroomLinesMatchTheirDensity() {
        assertTrue(checkKind(Structures.MUSHROOMS, 330L, 40, 1) >= 40);
    }

    @Test
    void anvilLinesMatchTheirDensity() {
        assertTrue(checkKind(Anvil.KIND, 331L, 12, 2) >= 12);
    }

    @Test
    void wornLinesMatchTheirDensity() {
        int worn = 0;
        for (int i = 0; i < 4000 && worn < 10; i++) {
            Structures.Mushroom m = Structures.MUSHROOMS.inCell(332L, 10 + i % 60, i / 60, LAND);
            if (m == null || m.wear() == 0) continue;
            worn++;
            assertEquals(0, disagreements(m, 1));
        }
        assertTrue(worn >= 10, "worn " + worn);
    }

    @Test
    void anvilLinesAwayFromTheStalkSkipToTheCap() {
        Anvil a = null;
        for (int i = 0; i < 4000 && a == null; i++) a = Anvil.KIND.inCell(333L, 10 + i % 60, i / 60, LAND);
        // Under the far side of the cap, past the stalk's reach, only the cap's few blocks are looked at.
        double dx = a.capX - a.centreX, dz = a.capZ - a.centreZ, d = Math.hypot(dx, dz);
        Shape.Line line = a.line(a.capX + dx / d * a.radius * 0.6, a.capZ + dz / d * a.radius * 0.6);
        assertTrue(line.maxY() - line.minY() < 30, "span " + (line.maxY() - line.minY()));
    }
}
