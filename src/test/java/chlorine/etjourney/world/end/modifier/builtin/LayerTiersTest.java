package chlorine.etjourney.world.end.modifier.builtin;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** LAYERED tiers keep their hole size everywhere. */
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
}
