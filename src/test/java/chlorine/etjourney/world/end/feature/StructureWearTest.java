package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Some stone structures come out worn: bites taken out where a 3D noise peaks. Wear only removes blocks, never adds
 * them, and chains stay whole.
 */
class StructureWearTest {

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

    private static final List<Structure.Kind<? extends Structure>> WORN_KINDS = Arrays.asList(
        Structures.MUSHROOMS,
        Structures.RINGLETS,
        Structures.CROSSES,
        Structures.ARCHES,
        Structures.SPIRAL_TOWERS,
        Structures.HOLLOW_PILLARS);

    private static int cell0(Structure.Kind<?> kind) {
        return (int) Math.ceil(Structures.MIN_RADIUS / kind.cell) + 1;
    }

    /** {solid with wear, solid without wear} over the block centres of the structure's box. */
    private static int[] solids(Structure s) {
        int worn = 0, whole = 0;
        for (int x = (int) Math.floor(s.minX()); x <= Math.ceil(s.maxX()); x++) {
            for (int z = (int) Math.floor(s.minZ()); z <= Math.ceil(s.maxZ()); z++) {
                for (int y = (int) Math.floor(s.minY()); y <= Math.ceil(s.maxY()); y++) {
                    double d = s.density(x + 0.5, y + 0.5, z + 0.5);
                    boolean inside = Math.hypot(x + 0.5 - s.centreX, z + 0.5 - s.centreZ) <= s.footprint;
                    double b = inside ? s.body(x + 0.5, y + 0.5, z + 0.5) : Double.NEGATIVE_INFINITY;
                    assertTrue(d < 0 || b >= 0, "wear added a block");
                    if (d >= 0) worn++;
                    if (b >= 0) whole++;
                }
            }
        }
        return new int[] { worn, whole };
    }

    @Test
    void aboutAThirdOfEachKindIsWorn() {
        long seed = 810;
        for (Structure.Kind<? extends Structure> kind : WORN_KINDS) {
            int c0 = cell0(kind), all = 0, worn = 0;
            for (int i = 0; i < 1600; i++) {
                Structure s = kind.inCell(seed, c0 + i % 40, c0 + i / 40, LAND);
                if (s == null) continue;
                all++;
                if (s.wear() > 0) worn++;
            }
            assertTrue(all > 20, kind.style + " formed " + all);
            double share = worn / (double) all;
            assertTrue(share > 0.2 && share < 0.5, kind.style + " worn share " + share);
            seed++;
        }
    }

    @Test
    void wearBitesAwayPartOfTheBody() {
        int c0 = cell0(Structures.MUSHROOMS);
        Structure worst = null;
        for (int i = 0; i < 1600; i++) {
            Structure s = Structures.MUSHROOMS.inCell(830, c0 + i % 40, c0 + i / 40, LAND);
            if (s != null && (worst == null || s.wear() > worst.wear())) worst = s;
        }
        assertNotNull(worst);
        assertTrue(worst.wear() > 0.8, "wear " + worst.wear());
        int[] n = solids(worst);
        double removed = 1 - n[0] / (double) n[1];
        assertTrue(removed > 0.1 && removed < 0.5, "removed " + removed);
    }

    @Test
    void lightWearStillBites() {
        int c0 = cell0(Structures.MUSHROOMS);
        Structure lightest = null;
        for (int i = 0; i < 1600; i++) {
            Structure s = Structures.MUSHROOMS.inCell(831, c0 + i % 40, c0 + i / 40, LAND);
            if (s != null && s.wear() > 0 && (lightest == null || s.wear() < lightest.wear())) lightest = s;
        }
        assertNotNull(lightest);
        int[] n = solids(lightest);
        assertTrue(n[0] < n[1], "nothing removed at wear " + lightest.wear());
    }

    @Test
    void chainsStayWhole() {
        int c0 = cell0(Structures.SKY_CHAINS);
        for (int i = 0; i < 400; i++) {
            Structure s = Structures.SKY_CHAINS.inCell(832, c0 + i % 20, c0 + i / 20, LAND);
            if (s != null) assertEquals(0, s.wear());
        }
    }
}
