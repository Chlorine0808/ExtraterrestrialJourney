package chlorine.etjourney.world.atmosphere;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Denser air only ever pulls the fog in, never past half the view, and without a jump as it starts. */
class FogMathTest {

    private static final float FAR = 192;

    @Test
    void densityOneKeepsVanilla() {
        assertEquals(192, FogMath.end(FAR, 1), 1e-4);
        assertEquals(144, FogMath.start(FAR, 1), 1e-4);
    }

    @Test
    void densityTwoEndsAtThreeQuarters() {
        assertEquals(144, FogMath.end(FAR, 2), 1e-4);
        assertEquals(36, FogMath.start(FAR, 2), 1e-4);
    }

    @Test
    void thinAirNeverPushesTheFogOut() {
        assertEquals(192, FogMath.end(FAR, 0.5f), 1e-4);
        assertEquals(144, FogMath.start(FAR, 0.5f), 1e-4);
    }

    @Test
    void fogNeverEndsNearerThanHalfTheView() {
        assertTrue(FogMath.end(FAR, 1000) >= 96);
    }

    @Test
    void startIsContinuousAcrossOne() {
        assertEquals(FogMath.start(FAR, 1), FogMath.start(FAR, 1.0001f), 0.1);
    }
}
