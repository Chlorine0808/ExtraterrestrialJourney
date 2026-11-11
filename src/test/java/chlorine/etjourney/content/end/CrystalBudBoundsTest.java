package chlorine.etjourney.content.end;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraftforge.common.util.ForgeDirection;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.plant.CrystalGrowth;
import chlorine.etjourney.world.end.modifier.EndBlock;

class CrystalBudBoundsTest {

    private static final float EPS = 1e-6F;

    private static CrystalBudBlock bud(EndBlock kind) {
        for (LargePlantBlockTable.Entry e : LargePlantBlockTable.ENTRIES) {
            if (e.kind == kind) return new CrystalBudBlock(e, null);
        }
        throw new IllegalArgumentException(kind.toString());
    }

    /** The box sits flush on its support face, reaches the bud's length out of it, and is centred across it. */
    @Test
    void boxSitsOnTheSupportFace() {
        CrystalBudBlock bud = bud(EndBlock.CRYSTAL_BUD_SMALL);
        float length = 3 / 16.0F, lo = 0.25F, hi = 0.75F;
        for (int facing = 0; facing < 6; facing++) {
            bud.setBlockBoundsBasedOnState(new FakeBlocks().meta(0, 64, 0, facing), 0, 64, 0);
            ForgeDirection s = ForgeDirection.getOrientation(CrystalGrowth.supportSide(facing));
            float[] min = { (float) bud.getBlockBoundsMinX(), (float) bud.getBlockBoundsMinY(),
                (float) bud.getBlockBoundsMinZ() };
            float[] max = { (float) bud.getBlockBoundsMaxX(), (float) bud.getBlockBoundsMaxY(),
                (float) bud.getBlockBoundsMaxZ() };
            int[] off = { s.offsetX, s.offsetY, s.offsetZ };
            for (int axis = 0; axis < 3; axis++) {
                String at = "facing " + facing + " axis " + axis;
                if (off[axis] > 0) {
                    assertEquals(1 - length, min[axis], EPS, at);
                    assertEquals(1, max[axis], EPS, at);
                } else if (off[axis] < 0) {
                    assertEquals(0, min[axis], EPS, at);
                    assertEquals(length, max[axis], EPS, at);
                } else {
                    assertEquals(lo, min[axis], EPS, at);
                    assertEquals(hi, max[axis], EPS, at);
                }
            }
        }
    }
}
