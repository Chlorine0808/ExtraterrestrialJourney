package chlorine.etjourney.world.end.region;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StyleTest {

    private static final Style PLAINS = Style.builder("PLAINS", StyleKind.BASE_LAND)
        .share(3)
        .build();
    private static final Style ISLETS = Style.builder("ISLETS", StyleKind.BASE_VOID)
        .share(1)
        .build();

    @Test
    void landOverlaySitsOnLandOnly() {
        Style spires = Style.builder("SPIRES", StyleKind.OVERLAY)
            .overlay(0.3, Style.LAND)
            .build();
        assertTrue(spires.canOverlay(PLAINS));
        assertFalse(spires.canOverlay(ISLETS));
        assertFalse(spires.canOverlay(spires));
    }

    @Test
    void basesWithoutOverlaySettingsNeverOverlay() {
        assertFalse(ISLETS.canOverlay(PLAINS));
        assertFalse(PLAINS.canOverlay(ISLETS));
    }

    @Test
    void namedTargetsAcceptOnlyThoseNames() {
        Style layered = Style.builder("LAYERED", StyleKind.BOTH)
            .share(1)
            .overlay(0.2, Style.named("PLAINS"))
            .build();
        Style basin = Style.builder("BASIN", StyleKind.BASE_LAND)
            .share(1)
            .build();
        assertTrue(layered.canOverlay(PLAINS));
        assertFalse(layered.canOverlay(basin));
        assertTrue(layered.hasLand() && layered.isBase());
    }
}
