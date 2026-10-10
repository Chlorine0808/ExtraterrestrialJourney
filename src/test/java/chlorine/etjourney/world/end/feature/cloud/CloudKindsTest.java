package chlorine.etjourney.world.end.feature.cloud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.Structure;
import chlorine.etjourney.world.end.feature.StructureProbe;
import chlorine.etjourney.world.end.feature.Structures;

/** Every cloud structure forms only where its style is, inside the world, its footprint and the outer End. */
class CloudKindsTest {

    /** Each task adds its kind here. */
    static final List<Structure.Kind<? extends Structure>> KINDS = Arrays
        .asList(Cumulus.KIND, Mammatus.KIND, Anvil.KIND);

    /** Land with an underside, so the kinds that stand on or hang from the land form too. */
    private static StructureProbe at(double weight) {
        return Probes.of(weight, 60, 70, 50);
    }

    @Test
    void nothingAtWeightZero() {
        long seed = 700;
        for (Structure.Kind<? extends Structure> kind : KINDS) {
            assertEquals(0, Probes.count(kind, seed++, at(0)), kind.style);
        }
    }

    @Test
    void somethingAtFullWeight() {
        long seed = 720;
        for (Structure.Kind<? extends Structure> kind : KINDS) {
            assertTrue(Probes.count(kind, seed++, at(1)) > 0, kind.style);
        }
    }

    @Test
    void insideTheWorldAndTheFootprint() {
        long seed = 740;
        for (Structure.Kind<? extends Structure> kind : KINDS) {
            int c0 = Probes.cell0(kind);
            for (int i = 0; i < 400; i++) {
                Structure s = kind.inCell(seed, c0 + i % 20, c0 + i / 20, at(1));
                if (s == null) continue;
                assertTrue(s.maxY() <= 250 && s.minY() >= 0, kind.style + " height " + s.minY() + ".." + s.maxY());
                assertTrue(Math.hypot(s.centreX, s.centreZ) >= Structures.MIN_RADIUS, kind.style);
                // Nothing solid just outside the footprint.
                double y = (s.minY() + s.maxY()) / 2;
                assertTrue(s.density(s.centreX + s.footprint + 1, y, s.centreZ) < 0, kind.style);
            }
            seed++;
        }
    }

    @Test
    void sameCellSameShape() {
        for (Structure.Kind<? extends Structure> kind : KINDS) {
            Structure a = Probes.first(kind, 760, at(1));
            assertNotNull(a, kind.style);
            Structure b = kind
                .inCell(760, (int) Math.floor(a.centreX / kind.cell), (int) Math.floor(a.centreZ / kind.cell), at(1));
            assertSame(a, b, kind.style);
        }
    }
}
