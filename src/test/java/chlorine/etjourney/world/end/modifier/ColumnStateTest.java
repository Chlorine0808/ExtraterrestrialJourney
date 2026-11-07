package chlorine.etjourney.world.end.modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The topmost surface of a column, stacked slabs included. */
class ColumnStateTest {

    @Test
    void surfaceIsTheTopWithoutLayers() {
        ColumnState c = new ColumnState(0, 0);
        c.top = 64;
        assertEquals(64, c.surface());
    }

    @Test
    void surfaceIsTheHighestLayerAboveTheTop() {
        ColumnState c = new ColumnState(0, 0);
        c.top = 64;
        c.layers.add(new Layer(110, 104));
        c.layers.add(new Layer(85, 80));
        assertEquals(110, c.surface());
    }
}
