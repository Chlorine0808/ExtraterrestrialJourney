package chlorine.etjourney.world.building;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;

import org.junit.jupiter.api.Test;

/** Turning templates about their anchor, clockwise seen from above. */
class RotationTest {

    /** 3 wide (x), 2 high, 5 deep (z), anchored at its bottom centre (1, 0, 2). */
    private static Template box() throws TemplateException {
        String row3 = "\"AAA\"";
        String rows = "[" + row3 + "," + row3 + "," + row3 + "," + row3 + "," + row3 + "]";
        String json = "[{\"layers\":[{\"rows\":" + rows + "},{\"rows\":" + rows + "}],\"mappings\":{\"A\":\"m:a\"}}]";
        return TemplateParser.parse("box", new StringReader(json), true);
    }

    @Test
    void anchorStaysPut() throws Exception {
        Template t = box();
        for (Rotation r : Rotation.values())
            assertArrayEquals(new int[] { 0, 0, 0 }, r.toWorld(t, t.anchorX, t.anchorY, t.anchorZ));
    }

    @Test
    void r90TurnsClockwiseSeenFromAbove() throws Exception {
        Template t = box();
        // One block east of the anchor ends up one block south.
        assertArrayEquals(new int[] { 0, 0, 1 }, Rotation.R90.toWorld(t, t.anchorX + 1, 0, t.anchorZ));
        // One block south ends up one block west.
        assertArrayEquals(new int[] { -1, 0, 0 }, Rotation.R90.toWorld(t, t.anchorX, 0, t.anchorZ + 1));
    }

    @Test
    void toLocalInvertsToWorld() throws Exception {
        Template t = box();
        for (Rotation r : Rotation.values())
            for (int x = 0; x < t.sizeX; x++) for (int y = 0; y < t.sizeY; y++) for (int z = 0; z < t.sizeZ; z++) {
                int[] w = r.toWorld(t, x, y, z);
                assertArrayEquals(new int[] { x, y, z }, r.toLocal(t, w[0], w[1], w[2]));
            }
    }

    @Test
    void fourQuarterTurnsIsIdentity() throws Exception {
        Template t = box();
        // Turning a relative offset four times by R90 brings it back.
        int[] d = { 2, 1, -1 };
        int[] p = d;
        for (int i = 0; i < 4; i++) p = Rotation.R90.turn(p[0], p[1], p[2]);
        assertArrayEquals(d, p);
        assertArrayEquals(Rotation.R180.toWorld(t, 0, 0, 0), Rotation.R90.turn(Rotation.R90.toWorld(t, 0, 0, 0)));
    }

    @Test
    void boundsSwapAxesOnQuarterTurns() throws Exception {
        Template t = box();
        int[] b0 = Rotation.R0.rotatedBounds(t);
        assertArrayEquals(new int[] { -1, -2, 1, 2 }, b0);
        int[] b90 = Rotation.R90.rotatedBounds(t);
        assertEquals(5, b90[2] - b90[0] + 1);
        assertEquals(3, b90[3] - b90[1] + 1);
    }

    @Test
    void ofDegrees() {
        assertEquals(Rotation.R270, Rotation.of(270));
        assertEquals(3, Rotation.R270.quarterTurns());
        assertThrows(IllegalArgumentException.class, () -> Rotation.of(45));
    }
}
