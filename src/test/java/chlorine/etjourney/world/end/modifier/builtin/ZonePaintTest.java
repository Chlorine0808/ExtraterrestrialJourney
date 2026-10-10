package chlorine.etjourney.world.end.modifier.builtin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.Zone;
import chlorine.etjourney.world.end.modifier.BlockSink;
import chlorine.etjourney.world.end.modifier.EndBlock;

/** Zone paint of a column split at Y 127/128: only the real top surface is painted. */
class ZonePaintTest {

    /** One column, Y minY to maxY - 1, filled with stone from fromY to toY. */
    private static final class Column implements BlockSink {

        final int minY;
        final EndBlock[] blocks;

        Column(int minY, int maxY, int fromY, int toY) {
            this.minY = minY;
            this.blocks = new EndBlock[maxY - minY];
            for (int y = Math.max(minY, fromY); y <= Math.min(maxY - 1, toY); y++) blocks[y - minY] = EndBlock.STONE;
        }

        @Override
        public int originX() {
            return 0;
        }

        @Override
        public int originZ() {
            return 0;
        }

        @Override
        public int minY() {
            return minY;
        }

        @Override
        public int maxY() {
            return minY + blocks.length;
        }

        @Override
        public boolean isAir(int x, int y, int z) {
            return blocks[y - minY] == null;
        }

        @Override
        public void place(int x, int y, int z, EndBlock block) {
            if (isAir(x, y, z)) blocks[y - minY] = block;
        }

        @Override
        public void set(int x, int y, int z, EndBlock block) {
            blocks[y - minY] = block;
        }

        @Override
        public void clear(int x, int y, int z) {
            blocks[y - minY] = null;
        }
    }

    @Test
    void theLowerHalfLeavesARunThatContinuesAboveUnpainted() {
        Column lower = new Column(0, 128, 100, 160);
        FeatureModifiers.paintColumn(lower, 0, 0, Zone.SOLAR, true, y -> true);
        assertEquals(EndBlock.STONE, lower.blocks[127]);
        Column upper = new Column(128, 256, 100, 160);
        FeatureModifiers.paintColumn(upper, 0, 0, Zone.SOLAR, false, y -> true);
        assertEquals(Zone.SOLAR.top, upper.blocks[160 - 128]);
        assertEquals(Zone.SOLAR.fill, upper.blocks[159 - 128]);
        assertEquals(EndBlock.STONE, upper.blocks[157 - 128]);
    }

    @Test
    void anIslandBelowTheCeilingIsPaintedInTheLowerHalf() {
        Column lower = new Column(0, 128, 60, 90);
        FeatureModifiers.paintColumn(lower, 0, 0, Zone.NEBULA, false, y -> true);
        assertEquals(Zone.NEBULA.top, lower.blocks[90]);
        assertEquals(EndBlock.STONE, lower.blocks[87]);
    }

    @Test
    void landOutsideTheIslandIsLeftAlone() {
        Column column = new Column(0, 128, 60, 70);
        FeatureModifiers.paintColumn(column, 0, 0, Zone.SOLAR, false, y -> false);
        for (int y = 60; y <= 70; y++) assertEquals(EndBlock.STONE, column.blocks[y], "at " + y);
    }

    @Test
    void theIslandUnderSomethingElseIsPaintedAtItsOwnTop() {
        // Island rock up to Y 90 with another shape's blocks on top of it up to Y 100.
        Column column = new Column(0, 128, 60, 100);
        FeatureModifiers.paintColumn(column, 0, 0, Zone.SOLAR, false, y -> y <= 90);
        assertEquals(EndBlock.STONE, column.blocks[100]);
        assertEquals(EndBlock.STONE, column.blocks[91]);
        assertEquals(Zone.SOLAR.top, column.blocks[90]);
        assertEquals(Zone.SOLAR.fill, column.blocks[89]);
        assertEquals(EndBlock.STONE, column.blocks[87]);
    }
}
