package chlorine.etjourney.world.end.shoot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.BiosphereSource;

class SpherePickerTest {

    private static Map<SphereClass, Integer> wanted(SphereClass c, int n) {
        Map<SphereClass, Integer> m = new EnumMap<>(SphereClass.class);
        m.put(c, n);
        return m;
    }

    @Test
    void classFollowsTheResolvedSite() {
        assertEquals(SphereClass.WATER, SphereClass.of(BiosphereSource.Kind.SURFACE, true));
        assertEquals(SphereClass.SURFACE, SphereClass.of(BiosphereSource.Kind.SURFACE, false));
        assertEquals(SphereClass.NETHER, SphereClass.of(BiosphereSource.Kind.NETHER, true));
        assertEquals(SphereClass.CAVE, SphereClass.of(BiosphereSource.Kind.CAVE, false));
    }

    @Test
    void takesUpToTheWantedCountPerClass() {
        SpherePicker<String> p = new SpherePicker<>(wanted(SphereClass.CAVE, 2));
        p.offer("a", SphereClass.CAVE);
        p.offer("b", SphereClass.SURFACE);
        p.offer("c", SphereClass.CAVE);
        p.offer("d", SphereClass.CAVE);
        assertEquals(Arrays.asList("a", "c"), p.picked(SphereClass.CAVE));
        assertTrue(
            p.picked(SphereClass.SURFACE)
                .isEmpty());
        assertTrue(p.full());
    }

    @Test
    void shortfallIsReported() {
        SpherePicker<String> p = new SpherePicker<>(wanted(SphereClass.WATER, 2));
        p.offer("a", SphereClass.WATER);
        assertFalse(p.full());
        assertEquals(1, p.shortfall(SphereClass.WATER));
    }

    @Test
    void wantsOnlyClassesWithRoomLeft() {
        SpherePicker<String> p = new SpherePicker<>(wanted(SphereClass.CAVE, 1));
        assertTrue(p.wants(SphereClass.CAVE));
        assertFalse(p.wants(SphereClass.WATER));
        p.offer("a", SphereClass.CAVE);
        assertFalse(p.wants(SphereClass.CAVE));
    }
}
