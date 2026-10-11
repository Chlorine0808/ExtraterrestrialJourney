package chlorine.etjourney.world.end.region;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The cloud styles have the kinds and overlay targets of the spec's table. */
class CloudStylesTest {

    @Test
    void stratusOverlaysLandOnlyAndIsNoBase() {
        assertEquals(StyleKind.OVERLAY, Styles.STRATUS.kind);
        assertEquals(0, Styles.STRATUS.share);
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

    @Test
    void lenticularsOverlayAnything() {
        assertEquals(StyleKind.OVERLAY, Styles.LENTICULARS.kind);
        assertTrue(Styles.LENTICULARS.canOverlay(Styles.ISLETS));
        assertTrue(
            Styles.all()
                .contains(Styles.LENTICULARS));
    }

    @Test
    void mackerelIsAVoidBaseOnly() {
        assertEquals(StyleKind.BASE_VOID, Styles.MACKEREL.kind);
        assertEquals(1, Styles.MACKEREL.share);
        assertFalse(Styles.MACKEREL.canOverlay(Styles.ISLETS));
        assertTrue(
            Styles.all()
                .contains(Styles.MACKEREL));
    }

    @Test
    void virgaOverlaysLandOnly() {
        assertEquals(StyleKind.OVERLAY, Styles.VIRGA.kind);
        assertTrue(Styles.VIRGA.canOverlay(Styles.PLAINS));
        assertFalse(Styles.VIRGA.canOverlay(Styles.ISLETS));
        assertTrue(
            Styles.all()
                .contains(Styles.VIRGA));
    }

    @Test
    void billowsOverlayAnythingAndAreNoBase() {
        assertEquals(StyleKind.OVERLAY, Styles.BILLOWS.kind);
        assertEquals(0, Styles.BILLOWS.share);
        assertTrue(Styles.BILLOWS.canOverlay(Styles.PLAINS));
        assertTrue(Styles.BILLOWS.canOverlay(Styles.ISLETS));
        assertTrue(
            Styles.all()
                .contains(Styles.BILLOWS));
    }
}
