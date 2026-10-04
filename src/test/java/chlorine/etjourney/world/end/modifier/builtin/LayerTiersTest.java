package chlorine.etjourney.world.end.modifier.builtin;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** LAYERED tiers keep their hole size everywhere and change character without steps. */
class LayerTiersTest {

    private static final long SALT = 0x5EEDL;

    private static int crossings(double x0, double phase) {
        int n = 0;
        boolean was = StyleModifiers.tier(SALT, 0, phase, x0, 0).patch > 0;
        for (int x = 1; x < 10000; x++) {
            boolean is = StyleModifiers.tier(SALT, 0, phase, x0 + x, 0).patch > 0;
            if (is != was) n++;
            was = is;
        }
        return n;
    }

    @Test
    void holeSizeDoesNotDependOnDistanceFromTheOrigin() {
        int near = crossings(2000, 0.5), far = crossings(60000, 0.5);
        assertTrue(far < near * 1.6, "holes far out are finer: " + far + " crossings against " + near);
    }

    @Test
    void tiersChangeCharacterWithoutSteps() {
        double worstThick = 0, worstOffset = 0;
        double prevThick = 0, prevOffset = 0;
        for (int x = 0; x < 30000; x++) {
            double phase = StyleModifiers.tierPhase(SALT, x, 0);
            StyleModifiers.Tier t = StyleModifiers.tier(SALT, 0, phase, x, 0);
            double thick = t.thick * Math.min(1, Math.max(0, t.patch));
            if (x > 0) {
                worstThick = Math.max(worstThick, Math.abs(thick - prevThick));
                worstOffset = Math.max(worstOffset, Math.abs(t.offset - prevOffset));
            }
            prevThick = thick;
            prevOffset = t.offset;
        }
        assertTrue(worstThick < 1.5, "thickness jumps by " + worstThick + " between neighbouring columns");
        assertTrue(worstOffset < 1.5, "height jumps by " + worstOffset + " between neighbouring columns");
    }
}
