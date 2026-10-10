package chlorine.etjourney.world.end.region;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The cloud styles have the kinds and overlay targets of the spec's table. */
class CloudStylesTest {

    @Test
    void stratusIsALandBaseThatAlsoOverlaysLand() {
        assertEquals(StyleKind.BOTH, Styles.STRATUS.kind);
        assertEquals(1, Styles.STRATUS.share);
        assertTrue(Styles.STRATUS.canOverlay(Styles.PLAINS));
        assertFalse(Styles.STRATUS.canOverlay(Styles.ISLETS));
        assertTrue(
            Styles.all()
                .contains(Styles.STRATUS));
    }

    @Test
    void cirrusOverlaysAnything() {
        assertEquals(StyleKind.OVERLAY, Styles.CIRRUS.kind);
        assertTrue(Styles.CIRRUS.canOverlay(Styles.PLAINS));
        assertTrue(Styles.CIRRUS.canOverlay(Styles.ISLETS));
        assertTrue(
            Styles.all()
                .contains(Styles.CIRRUS));
    }
}
