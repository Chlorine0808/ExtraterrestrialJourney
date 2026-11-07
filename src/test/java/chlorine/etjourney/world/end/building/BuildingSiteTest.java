package chlorine.etjourney.world.end.building;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.function.DoubleBinaryOperator;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.StructureProbe;

/** Where a standing building's bottom layer goes on its five measured points. */
class BuildingSiteTest {

    static StructureProbe ground(DoubleBinaryOperator height) {
        return ground(height, false);
    }

    static StructureProbe ground(DoubleBinaryOperator height, boolean wet) {
        return new StructureProbe() {

            @Override
            public double weight(String style, double x, double z) {
                return 1;
            }

            @Override
            public double land(double x, double z) {
                return 1;
            }

            @Override
            public double ground(double x, double z) {
                return height.applyAsDouble(x, z);
            }

            @Override
            public boolean wet(double x, double z, double range) {
                return wet;
            }
        };
    }

    @Test
    void flatGroundSinksTwo() {
        // Ground 64 is the top solid block, so the bottom layer takes y 63 and 64.
        assertEquals(63, BuildingSite.groundY(ground((x, z) -> 64), 0, 0, 10, 10, 6, 2));
    }

    @Test
    void sinksFromTheHighestPoint() {
        // The low corner stands on its foundation instead of burying the high side.
        assertEquals(63, BuildingSite.groundY(ground((x, z) -> x == 0 && z == 0 ? 60 : 64), 0, 0, 10, 10, 6, 2));
        assertEquals(67, BuildingSite.groundY(ground((x, z) -> x == 0 && z == 0 ? 68 : 64), 0, 0, 10, 10, 6, 2));
    }

    @Test
    void slopeOverLimitRejects() {
        assertNull(BuildingSite.groundY(ground((x, z) -> x == 10 && z == 10 ? 71 : 64), 0, 0, 10, 10, 6, 2));
        assertEquals(69, BuildingSite.groundY(ground((x, z) -> x == 10 && z == 10 ? 70 : 64), 0, 0, 10, 10, 6, 2));
    }

    @Test
    void wetOrVoidRejects() {
        assertNull(BuildingSite.groundY(ground((x, z) -> 64, true), 0, 0, 10, 10, 6, 2));
        assertNull(BuildingSite.groundY(ground((x, z) -> x == 5 && z == 5 ? -1000 : 64), 0, 0, 10, 10, 6, 2));
    }
}
