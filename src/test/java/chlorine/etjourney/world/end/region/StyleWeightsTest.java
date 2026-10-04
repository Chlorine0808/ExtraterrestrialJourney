package chlorine.etjourney.world.end.region;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class StyleWeightsTest {

    private static final Style LOWLANDS = Style.builder("LOWLANDS", StyleKind.BASE_LAND)
        .share(1)
        .mountains(0.2)
        .build();
    private static final Style BASIN = Style.builder("BASIN", StyleKind.BASE_LAND)
        .share(1)
        .mountains(0.3)
        .build();

    @Test
    void borderScaleStaysBetweenItsStyles() {
        Map<Style, Double> map = new HashMap<>();
        map.put(LOWLANDS, 0.5);
        map.put(BASIN, 0.5);
        double scale = StyleWeights.of(map)
            .mountainScale();
        assertTrue(scale >= 0.2 && scale <= 0.3, "scale " + scale);
    }

    @Test
    void overlaysAddOnTopOfAFullBase() {
        Map<Style, Double> map = new HashMap<>();
        map.put(RegionPickerTest.PLAINS, 1.0);
        map.put(RegionPickerTest.SPIRES, 0.8);
        StyleWeights w = StyleWeights.of(map);
        assertEquals(1.0, w.of(RegionPickerTest.PLAINS), 1e-9);
        assertEquals(0.8, w.of(RegionPickerTest.SPIRES), 1e-9);
    }

    @Test
    void voidShareCountsVoidBases() {
        Map<Style, Double> map = new HashMap<>();
        map.put(RegionPickerTest.ISLETS, 0.7);
        map.put(RegionPickerTest.PLAINS, 0.3);
        assertEquals(
            0.7,
            StyleWeights.of(map)
                .voidShare(),
            1e-9);
    }
}
