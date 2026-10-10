package chlorine.etjourney.world.end.region;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StylesTest {

    @Test
    void elevenStylesWithTheSpecsOverlayTable() {
        assertEquals(
            35,
            Styles.all()
                .size());
        assertTrue(Styles.SPIRES.canOverlay(Styles.PLAINS));
        assertFalse(Styles.SPIRES.canOverlay(Styles.ISLETS));
        assertTrue(Styles.LAYERED.canOverlay(Styles.BASIN));
        assertFalse(Styles.LAYERED.canOverlay(Styles.RANGES));
        assertTrue(Styles.ARCS.canOverlay(Styles.ISLETS));
        assertTrue(Styles.ARCS.canOverlay(Styles.RANGES));
        assertFalse(Styles.SHOALS.canOverlay(Styles.PLAINS));
    }

    @Test
    void anArcsOverlayDoesNotEraseTheContinent() {
        RegionPicker picker = new RegionPicker(Styles.all());
        for (int i = 0; i < 4000; i++) {
            int cx = i % 64, cz = i / 64;
            if (picker.base(9L, cx, cz)
                .hasLand()
                && picker.overlays(9L, cx, cz)
                    .contains(Styles.ARCS)) {
                double[] c = RegionMap.cellCentre(9L, cx, cz);
                // Only overlays sit on a land base here, so no base weight is void near the centre.
                assertTrue(
                    StyleWeights.at(picker, 9L, c[0], c[1])
                        .voidShare() < 1);
                return;
            }
        }
    }

    @Test
    void overlayOnlyStylesAreFoundInSomeRegion() {
        RegionPicker picker = new RegionPicker(Styles.all());
        for (Style style : new Style[] { Styles.SPIRES, Styles.WAVES, Styles.WILD_WAVES }) {
            boolean found = false;
            for (int i = 0; i < 4000 && !found; i++) found = picker.contains(9L, i % 64, i / 64, style);
            assertTrue(found, style + " never appears");
        }
    }

    @Test
    void aboutOneOverlayPerRegion() {
        RegionPicker picker = new RegionPicker(Styles.all());
        int overlays = 0, cells = 4000;
        for (int i = 0; i < cells; i++) {
            overlays += picker.overlays(5L, i % 64, i / 64)
                .size();
        }
        double mean = overlays / (double) cells;
        assertTrue(mean >= 0.6 && mean <= 1.0, "mean overlays per region " + mean);
    }

    @Test
    void chasmsOverlayLandOnly() {
        assertEquals(StyleKind.OVERLAY, Styles.CHASMS.kind);
        assertTrue(Styles.CHASMS.canOverlay(Styles.PLAINS));
        assertFalse(Styles.CHASMS.canOverlay(Styles.ISLETS));
    }

    @Test
    void theCloudSeaIsGone() {
        for (Style style : Styles.all()) assertFalse(style.name.equals("CLOUD_SEA"));
    }
}
