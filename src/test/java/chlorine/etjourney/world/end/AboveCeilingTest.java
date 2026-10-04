package chlorine.etjourney.world.end;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.modifier.ColumnState;
import chlorine.etjourney.world.end.modifier.Layer;
import chlorine.etjourney.world.end.modifier.Shape;
import chlorine.etjourney.world.end.region.RegionPicker;
import chlorine.etjourney.world.end.region.Styles;

/** Terrain above the generator's Y 127: shapes and layers wake the tall pass, and block work runs up there too. */
class AboveCeilingTest {

    private static Shape shapeUpTo(double maxY) {
        return new Shape() {

            @Override
            public double minX() {
                return 0;
            }

            @Override
            public double maxX() {
                return 16;
            }

            @Override
            public double minZ() {
                return 0;
            }

            @Override
            public double maxZ() {
                return 16;
            }

            @Override
            public double minY() {
                return 40;
            }

            @Override
            public double maxY() {
                return maxY;
            }

            @Override
            public double density(double x, double y, double z) {
                return -1;
            }
        };
    }

    private static List<ColumnState> lowColumns() {
        ColumnState c = new ColumnState(0, 0);
        c.top = 70;
        return Collections.singletonList(c);
    }

    @Test
    void shapesAndLayersAboveTheCeilingNeedTheTallPass() {
        assertFalse(TallPass.needed(lowColumns(), Collections.singletonList(shapeUpTo(100))));
        assertTrue(TallPass.needed(lowColumns(), Collections.singletonList(shapeUpTo(200))));
        List<ColumnState> layered = lowColumns();
        layered.get(0).layers.add(new Layer(180, 170));
        assertTrue(TallPass.needed(layered, Collections.emptyList()));
    }

    @Test
    void blockWorkRunsOnTheUpperHalfWithinItsRange() {
        TerrainSampler sampler = new TerrainSampler(61L, new RegionPicker(Styles.all()));
        for (int cx = 80; cx < 120; cx += 7) {
            MemorySink upper = new MemorySink(cx, 3, 128, 256);
            new ChunkPlan(sampler, cx, 3, (x, z) -> Collections.emptyList()).blocks(upper);
            assertTrue(upper.placedOnlyWithinRange());
        }
    }
}
