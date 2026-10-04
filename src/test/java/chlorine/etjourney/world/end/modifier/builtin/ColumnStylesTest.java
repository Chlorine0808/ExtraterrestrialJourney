package chlorine.etjourney.world.end.modifier.builtin;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.Underside;
import chlorine.etjourney.world.end.modifier.ColumnState;
import chlorine.etjourney.world.end.modifier.Layer;

/** MESAS, INVERTED, MIRRORED and SHATTERED on real continent columns. */
class ColumnStylesTest {

    @Test
    void mesasOnlyLowerTheSurfaceByLessThanAStep() {
        double x0 = BuiltinModifiersTest.landX();
        for (int i = 0; i < 60; i++) {
            ColumnState plain = BuiltinModifiersTest.run(x0 + i * 4, Collections.emptyList());
            ColumnState mesa = BuiltinModifiersTest.run(x0 + i * 4, Collections.singletonList(StyleModifiers.mesas()));
            // The smoothed riser may end a fraction of a block above the original surface.
            assertTrue(mesa.top <= plain.top + 1 && mesa.top > plain.top - 14, "at " + i);
        }
    }

    @Test
    void invertedHangsReliefBelowAndKeepsNoneAbove() {
        double x0 = BuiltinModifiersTest.landX();
        boolean hung = false;
        for (int i = 0; i < 80; i++) {
            ColumnState s = BuiltinModifiersTest.run(x0 + i * 8, Collections.singletonList(StyleModifiers.inverted()));
            assertTrue(Math.abs(s.rise) < 1e-9);
            if (s.hang > 1) hung = true;
        }
        assertTrue(hung, "no stalactites hung anywhere");
    }

    @Test
    void mirroredPutsASlabBelowTheContinent() {
        double x0 = BuiltinModifiersTest.landX();
        int mirrored = 0;
        for (int i = 0; i < 40; i++) {
            ColumnState s = BuiltinModifiersTest
                .run(x0 + i * 8, Arrays.asList(StyleModifiers.mirroredLift(), StyleModifiers.mirrored()));
            for (Layer layer : s.layers) {
                assertTrue(layer.top < s.bottom && layer.bottom < layer.top);
                mirrored++;
            }
        }
        assertTrue(mirrored > 0);
    }

    @Test
    void shatteredOpensCracksAndShiftsShards() {
        double x0 = BuiltinModifiersTest.landX();
        int cracks = 0, shifted = 0;
        for (int i = 0; i < 200; i++) {
            double x = x0 + i * 2;
            ColumnState plain = BuiltinModifiersTest.run(x, Collections.emptyList());
            ColumnState s = BuiltinModifiersTest.run(x, Collections.singletonList(StyleModifiers.shattered()));
            if (plain.land > 0 && s.land <= 0) cracks++;
            if (Math.abs(s.level - plain.level) > 3) shifted++;
        }
        assertTrue(cracks > 0, "no cracks");
        assertTrue(shifted > 0, "no shifted shards");
    }

    @Test
    void invertedHangsBelowWithoutLiftingTheSurface() {
        double x0 = BuiltinModifiersTest.landX();
        boolean hung = false;
        for (int i = 0; i < 120; i++) {
            ColumnState lifted = BuiltinModifiersTest
                .run(x0 + i * 4, Collections.singletonList(StyleModifiers.invertedLift()));
            ColumnState s = BuiltinModifiersTest
                .run(x0 + i * 4, Arrays.asList(StyleModifiers.invertedLift(), StyleModifiers.inverted()));
            // INVERTED only damps hills on top; the hung relief must not come back as a raised surface.
            assertTrue(
                Math.abs(s.top - lifted.top) <= Math.abs(lifted.hills) * 0.7 + 1,
                "at " + i + ": " + s.top + " vs " + lifted.top);
            assertTrue(s.bottom >= Underside.MIN_BOTTOM - 1e-9);
            if (s.hang > 1) hung = true;
        }
        assertTrue(hung, "no stalactites hung anywhere");
    }

    @Test
    void shatteredCracksAreWiderThanTheDensityGrid() {
        double x0 = BuiltinModifiersTest.landX();
        int crossings = 0, run = 0;
        boolean landBefore = false;
        for (int i = 0; i < 1500; i++) {
            double x = x0 + i;
            ColumnState plain = BuiltinModifiersTest.run(x, Collections.emptyList());
            ColumnState s = BuiltinModifiersTest.run(x, Collections.singletonList(StyleModifiers.shattered()));
            boolean cracked = plain.land > 0 && s.land <= 0;
            if (cracked) {
                run++;
                continue;
            }
            // A crossing between two stretches of land: perpendicular is the narrowest a crack can be crossed.
            if (run > 0 && landBefore && s.land > 0) {
                crossings++;
                assertTrue(run >= 11, "crack only " + run + " blocks wide at " + i);
            }
            run = 0;
            landBefore = s.land > 0;
        }
        assertTrue(crossings > 0, "no crack crossed");
    }
}
