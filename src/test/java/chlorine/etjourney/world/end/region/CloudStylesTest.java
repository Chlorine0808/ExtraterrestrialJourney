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

    @Test
    void cloudSeaIsALandBaseOnly() {
        assertEquals(StyleKind.BASE_LAND, Styles.CLOUD_SEA.kind);
        assertEquals(1, Styles.CLOUD_SEA.share);
        assertFalse(Styles.CLOUD_SEA.canOverlay(Styles.PLAINS));
        assertTrue(
            Styles.all()
                .contains(Styles.CLOUD_SEA));
    }

    @Test
    void cumulusOverlaysAnything() {
        assertEquals(StyleKind.OVERLAY, Styles.CUMULUS.kind);
        assertTrue(Styles.CUMULUS.canOverlay(Styles.ISLETS));
        assertTrue(Styles.CUMULUS.canOverlay(Styles.PLAINS));
        assertTrue(
            Styles.all()
                .contains(Styles.CUMULUS));
    }

    @Test
    void mammatusOverlaysLandOnly() {
        assertEquals(StyleKind.OVERLAY, Styles.MAMMATUS.kind);
        assertTrue(Styles.MAMMATUS.canOverlay(Styles.PLAINS));
        assertFalse(Styles.MAMMATUS.canOverlay(Styles.ISLETS));
        assertTrue(
            Styles.all()
                .contains(Styles.MAMMATUS));
    }

    @Test
    void anvilsOverlayAnything() {
        assertEquals(StyleKind.OVERLAY, Styles.ANVILS.kind);
        assertTrue(Styles.ANVILS.canOverlay(Styles.ISLETS));
        assertTrue(
            Styles.all()
                .contains(Styles.ANVILS));
    }
}
