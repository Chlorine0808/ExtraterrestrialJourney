package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Below the floor and above a ceiling of the sample world, bedrock included, a cut-out takes the outermost rock
 * of the column, so a ball reaching past them is not hollow there. Open sky stays air.
 */
class BedrockTest {

    /** Bedrock from 0 to floor and from ceiling up, solid rock from rockLow to rockHigh, air elsewhere. */
    private static Bedrock.Column column(int floor, int ceiling, int rockLow, int rockHigh) {
        return new Bedrock.Column() {

            @Override
            public boolean bedrock(int y) {
                return y >= 0 && y <= floor || y >= ceiling;
            }

            @Override
            public boolean solid(int y) {
                return !bedrock(y) && y >= rockLow && y <= rockHigh;
            }
        };
    }

    /** An overworld: bedrock up to 4, stone from 5 to 60, sky above. */
    private static final Bedrock.Column OVERWORLD = column(4, 1000, 5, 60);
    /** A Nether 128 high: bedrock up to 3 and from 123, netherrack from 4 to 120. */
    private static final Bedrock.Column NETHER = column(3, 123, 4, 120);

    @Test
    void blocksInsideTheWorldStayThemselves() {
        Bedrock.Rock rock = Bedrock.rock(OVERWORLD, 256);
        assertEquals(30, Bedrock.source(OVERWORLD, rock, 30, 256, false));
        assertEquals(70, Bedrock.source(OVERWORLD, rock, 70, 256, false));
    }

    @Test
    void floorBedrockAndBelowTakeTheLowestRock() {
        Bedrock.Rock rock = Bedrock.rock(OVERWORLD, 256);
        assertEquals(5, Bedrock.source(OVERWORLD, rock, 2, 256, false));
        assertEquals(5, Bedrock.source(OVERWORLD, rock, -20, 256, false));
    }

    @Test
    void openSkyStaysAir() {
        assertEquals(-1, Bedrock.source(OVERWORLD, Bedrock.rock(OVERWORLD, 256), 300, 256, false));
    }

    @Test
    void ceilingBedrockAndAboveTakeTheHighestRock() {
        Bedrock.Rock rock = Bedrock.rock(NETHER, 128);
        assertEquals(120, Bedrock.source(NETHER, rock, 125, 128, true));
        assertEquals(120, Bedrock.source(NETHER, rock, 140, 128, true));
        assertEquals(4, Bedrock.source(NETHER, rock, -5, 128, true));
    }

    @Test
    void aColumnWithoutRockFillsNothing() {
        Bedrock.Column hollow = column(4, 1000, 500, 400);
        assertEquals(-1, Bedrock.source(hollow, Bedrock.rock(hollow, 256), -3, 256, false));
    }
}
