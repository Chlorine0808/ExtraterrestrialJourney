package chlorine.etjourney.world.end.region;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class RegionPickerTest {

    static final Style PLAINS = Style.builder("PLAINS", StyleKind.BASE_LAND)
        .share(3)
        .build();
    static final Style ISLETS = Style.builder("ISLETS", StyleKind.BASE_VOID)
        .share(1)
        .build();
    static final Style SPIRES = Style.builder("SPIRES", StyleKind.OVERLAY)
        .overlay(0.5, Style.LAND)
        .build();
    static final Style ARCS = Style.builder("ARCS", StyleKind.OVERLAY)
        .overlay(0.5, Style.ANY)
        .build();
    static final Style WAVES = Style.builder("WAVES", StyleKind.OVERLAY)
        .overlay(0.5, Style.LAND)
        .build();

    @Test
    void overlaysRespectDeclarationsAndTheLimit() {
        RegionPicker picker = new RegionPicker(Arrays.asList(PLAINS, ISLETS, SPIRES, ARCS, WAVES));
        Map<Style, Integer> bases = new HashMap<>();
        for (int i = 0; i < 20000; i++) {
            int cx = i % 200, cz = i / 200;
            Style base = picker.base(3L, cx, cz);
            bases.merge(base, 1, Integer::sum);
            List<Style> overlays = picker.overlays(3L, cx, cz);
            assertTrue(overlays.size() <= RegionPicker.MAX_OVERLAYS);
            for (Style overlay : overlays) assertTrue(overlay.canOverlay(base), overlay + " on " + base);
        }
        assertEquals(0.75, bases.get(PLAINS) / 20000.0, 0.02);
    }
}
