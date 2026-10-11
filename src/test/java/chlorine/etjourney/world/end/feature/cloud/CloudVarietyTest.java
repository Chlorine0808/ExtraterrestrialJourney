package chlorine.etjourney.world.end.feature.cloud;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.ToDoubleFunction;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.Structure;
import chlorine.etjourney.world.end.feature.StructureProbe;

/** Clouds of one kind differ in size and proportion, rather than all coming out alike. */
class CloudVarietyTest {

    /** Largest over smallest of a measure, over the clouds of 40 x 40 cells. */
    static <T extends Structure> double spread(Structure.Kind<T> kind, long seed, StructureProbe probe,
        ToDoubleFunction<T> measure) {
        double low = Double.MAX_VALUE, high = 0;
        int c0 = Probes.cell0(kind);
        for (int i = 0; i < 1600; i++) {
            T s = kind.inCell(seed, c0 + i % 40, c0 + i / 40, probe);
            if (s == null) continue;
            double m = measure.applyAsDouble(s);
            low = Math.min(low, m);
            high = Math.max(high, m);
        }
        assertTrue(high > 0, kind.style + " formed nothing");
        return high / low;
    }

    @Test
    void cumulusComeInManySizesAndStretches() {
        double size = spread(Cumulus.KIND, 901L, Probes.LAND, c -> c.footprint);
        assertTrue(size >= 2.2, "footprint spread " + size);
        double stretch = spread(Cumulus.KIND, 901L, Probes.LAND, c -> c.stretch);
        assertTrue(stretch >= 1.4, "stretch spread " + stretch);
    }
}
