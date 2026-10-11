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

    @Test
    void anvilsComeInManySizesThicknessesAndStretches() {
        double radius = spread(Anvil.KIND, 902L, Probes.LAND, a -> a.radius);
        assertTrue(radius >= 2.2, "radius spread " + radius);
        double thick = spread(Anvil.KIND, 902L, Probes.LAND, a -> a.thick);
        assertTrue(thick >= 2.5, "thickness spread " + thick);
        double stretch = spread(Anvil.KIND, 902L, Probes.LAND, a -> a.stretch);
        assertTrue(stretch >= 1.3, "stretch spread " + stretch);
    }

    @Test
    void anvilRimsAreRagged() {
        Anvil a = Probes.first(Anvil.KIND, 903L, Probes.LAND);
        double y = a.capY + a.thick / 2, cos = Math.cos(a.heading), sin = Math.sin(a.heading);
        double widest = 0, narrowest = Double.MAX_VALUE;
        // The rim across the drift, where the stretch leaves the radius as it is.
        for (int k = -6; k <= 6; k++) {
            double t = Math.toRadians(90 + 4 * k), dx = Math.cos(t) * cos - Math.sin(t) * sin;
            double dz = Math.cos(t) * sin + Math.sin(t) * cos, r = 0;
            while (r < a.radius * 2 && a.density(a.capX + dx * r, y, a.capZ + dz * r) > 0) r += 0.25;
            widest = Math.max(widest, r);
            narrowest = Math.min(narrowest, r);
        }
        assertTrue(widest - narrowest >= 2, "rim from " + narrowest + " to " + widest);
    }

    @Test
    void mammatusComeInManySizesAndSags() {
        double radius = spread(Mammatus.KIND, 904L, Probes.LAND, m -> m.radius);
        assertTrue(radius >= 2.5, "radius spread " + radius);
        double sag = spread(Mammatus.KIND, 904L, Probes.LAND, m -> m.sag);
        assertTrue(sag >= 1.5, "sag spread " + sag);
    }

    @Test
    void mammatusAreLumpy() {
        Mammatus m = Probes.first(Mammatus.KIND, 905L, Probes.LAND);
        double low = Double.MAX_VALUE, high = -Double.MAX_VALUE;
        for (int k = 0; k < 16; k++) {
            double t = k * Math.PI / 8, d = m.density(
                m.centreX + Math.cos(t) * m.radius * 0.85,
                m.centreY,
                m.centreZ + Math.sin(t) * m.radius * 0.85);
            low = Math.min(low, d);
            high = Math.max(high, d);
        }
        assertTrue(high - low >= 0.5, "density round the pouch from " + low + " to " + high);
    }

    @Test
    void virgaComeInManyLengthsAndLeans() {
        double longest = spread(Virga.KIND, 906L, Probes.LAND, v -> v.longest);
        assertTrue(longest >= 2.5, "length spread " + longest);
        double lean = spread(Virga.KIND, 906L, Probes.LAND, v -> 0.01 + v.lean);
        assertTrue(lean >= 10, "lean spread " + lean);
    }
}
