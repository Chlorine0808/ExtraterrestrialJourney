package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ZoneTest {

    /** Existing worlds keep their zones: the map must not shift when the cell picking is reorganised. */
    @Test
    void zoneMapIsUnchanged() {
        StringBuilder map = new StringBuilder();
        for (int i = -20; i < 20; i++) {
            for (int j = -20; j < 20; j++) map.append(
                Zone.at(42L, i * 173.0, j * 211.0)
                    .ordinal());
        }
        assertEquals(
            -2078285751,
            map.toString()
                .hashCode());
    }
}
