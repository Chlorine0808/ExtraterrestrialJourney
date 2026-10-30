package chlorine.etjourney.world.end.feature.plant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.modifier.EndBlock;

class CrystalGrowthTest {

    @Test
    void stagesGoInOrder() {
        assertEquals(EndBlock.CRYSTAL_BUD_SMALL, CrystalGrowth.next(null));
        assertEquals(EndBlock.CRYSTAL_BUD_MEDIUM, CrystalGrowth.next(EndBlock.CRYSTAL_BUD_SMALL));
        assertEquals(EndBlock.CRYSTAL_BUD_LARGE, CrystalGrowth.next(EndBlock.CRYSTAL_BUD_MEDIUM));
        assertEquals(EndBlock.CRYSTAL_CLUSTER, CrystalGrowth.next(EndBlock.CRYSTAL_BUD_LARGE));
    }

    @Test
    void clusterIsTheLast() {
        assertNull(CrystalGrowth.next(EndBlock.CRYSTAL_CLUSTER));
        assertNull(CrystalGrowth.next(EndBlock.CRYSTAL));
        assertTrue(CrystalGrowth.GROW_CHANCE > 0 && CrystalGrowth.GROW_CHANCE < 1);
    }

    @Test
    void onlyClusterDropsShard() {
        for (EndBlock block : EndBlock.values()) {
            assertEquals(block == EndBlock.CRYSTAL_CLUSTER, CrystalGrowth.dropsShard(block), block.name());
        }
        assertFalse(CrystalGrowth.dropsShard(null));
    }

    @Test
    void budNeedsItsSupportFace() {
        // Forge order: DOWN=0, UP=1, NORTH=2, SOUTH=3, WEST=4, EAST=5.
        int[] opposite = { 1, 0, 3, 2, 5, 4 };
        for (int facing = 0; facing < 6; facing++) {
            assertEquals(opposite[facing], CrystalGrowth.supportSide(facing));
        }
        assertThrows(IllegalArgumentException.class, () -> CrystalGrowth.supportSide(-1));
        assertThrows(IllegalArgumentException.class, () -> CrystalGrowth.supportSide(6));
    }
}
