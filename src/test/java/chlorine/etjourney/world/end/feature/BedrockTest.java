package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Bedrock in a sample gives way to the nearest rock beside it in the column, toward the middle of the world. */
class BedrockTest {

    /** Bedrock from low to high, solid rock from rockLow to rockHigh, air elsewhere. */
    private static Bedrock.Column column(int low, int high, int rockLow, int rockHigh) {
        return new Bedrock.Column() {

            @Override
            public boolean bedrock(int y) {
                return y >= low && y <= high;
            }

            @Override
            public boolean solid(int y) {
                return !bedrock(y) && y >= rockLow && y <= rockHigh;
            }
        };
    }

    @Test
    void floorBedrockTakesTheRockAboveIt() {
        assertEquals(5, Bedrock.standIn(column(0, 4, 5, 60), 2, 256));
    }

    @Test
    void ceilingBedrockTakesTheRockBelowIt() {
        // A Nether 128 high with bedrock from 123 and netherrack up to 120.
        assertEquals(120, Bedrock.standIn(column(123, 127, 0, 120), 125, 128));
    }

    @Test
    void otherBlocksStayThemselves() {
        assertEquals(30, Bedrock.standIn(column(0, 4, 5, 60), 30, 256));
    }

    @Test
    void bedrockWithNoRockNearbyBecomesAir() {
        assertEquals(-1, Bedrock.standIn(column(0, 4, 40, 60), 2, 256));
    }
}
