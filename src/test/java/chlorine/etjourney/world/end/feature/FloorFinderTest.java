package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The floor chosen is the one most columns around the centre share, not a pillar under the centre. */
class FloorFinderTest {

    /** Solid up to the height the function gives, air above; nothing is liquid. */
    private interface Height {

        int at(int x, int z);
    }

    private static FloorFinder.Blocks terrain(Height top) {
        return new FloorFinder.Blocks() {

            @Override
            public boolean ground(int x, int y, int z) {
                return y <= top.at(x, z);
            }

            @Override
            public boolean air(int x, int y, int z) {
                return y > top.at(x, z);
            }
        };
    }

    @Test
    void aBroadFloorBeatsAPillarUnderTheCentre() {
        FloorFinder.Blocks w = terrain((x, z) -> Math.abs(x) <= 1 && Math.abs(z) <= 1 ? 100 : 50);
        assertEquals(50, FloorFinder.broadest(w, 0, 0, 32, 6, 120));
    }

    @Test
    void aFlatFloorIsFound() {
        assertEquals(70, FloorFinder.broadest(terrain((x, z) -> 70), 0, 0, 32, 6, 120));
    }

    @Test
    void theLowerBroadFloorBeatsANarrowLedge() {
        // A ledge at 80 over a third of the columns, the floor at 40 under the rest.
        FloorFinder.Blocks w = terrain((x, z) -> x > 10 ? 80 : 40);
        assertEquals(40, FloorFinder.broadest(w, 0, 0, 32, 6, 120));
    }

    @Test
    void noFloorInSolidRock() {
        assertEquals(-1, FloorFinder.broadest(terrain((x, z) -> 255), 0, 0, 32, 6, 120));
    }

    @Test
    void noFloorInOpenAir() {
        assertEquals(-1, FloorFinder.broadest(terrain((x, z) -> -10), 0, 0, 32, 6, 120));
    }

    @Test
    void anEmptyRangeHasNoFloor() {
        // A cave under a low surface can leave the highest floor to look at below the lowest.
        assertEquals(-1, FloorFinder.broadest(terrain((x, z) -> 70), 0, 0, 32, 6, 2));
    }

    @Test
    void aFloorUnderALowCeilingIsSkipped() {
        // Floor at 60 with a ceiling from 64: three blocks of room, short of the six needed.
        FloorFinder.Blocks w = new FloorFinder.Blocks() {

            @Override
            public boolean ground(int x, int y, int z) {
                return y <= 60 || y >= 64;
            }

            @Override
            public boolean air(int x, int y, int z) {
                return !ground(x, y, z);
            }
        };
        assertEquals(-1, FloorFinder.broadest(w, 0, 0, 32, 6, 120));
    }

    /** Rock everywhere but a chamber from Y 20 to 40 within 20 blocks of the centre. */
    private static final FloorFinder.Blocks CHAMBER = new FloorFinder.Blocks() {

        @Override
        public boolean ground(int x, int y, int z) {
            return !air(x, y, z);
        }

        @Override
        public boolean air(int x, int y, int z) {
            return y >= 20 && y <= 40 && Math.abs(x) <= 20 && Math.abs(z) <= 20;
        }
    };

    @Test
    void aCaveBallCentresOnTheBiggestChamber() {
        // The ball's centre sits rise blocks above the floor height returned.
        int floor = FloorFinder.roomiest(CHAMBER, 0, 0, 32, 8, 6, 56);
        assertTrue(floor + 8 >= 26 && floor + 8 <= 34, "centre at " + (floor + 8));
    }

    @Test
    void solidRockHasNoRoomiestHeight() {
        assertEquals(-1, FloorFinder.roomiest(terrain((x, z) -> 255), 0, 0, 32, 8, 6, 56));
    }

    @Test
    void anEmptyRangeHasNoRoomiestHeight() {
        assertEquals(-1, FloorFinder.roomiest(CHAMBER, 0, 0, 32, 8, 6, 2));
    }
}
