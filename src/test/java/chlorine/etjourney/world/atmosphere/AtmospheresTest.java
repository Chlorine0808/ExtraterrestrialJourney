package chlorine.etjourney.world.atmosphere;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.EndBiome;

/** Biomes map to atmospheres through a byte ID that survives the trip to the client. */
class AtmospheresTest {

    @Test
    void nullBiomeIsNone() {
        assertSame(Atmospheres.NONE, Atmospheres.byId(Atmospheres.idOf(null)));
    }

    @Test
    void everyBiomeRoundTripsToItsOwnAtmosphere() {
        for (EndBiome a : EndBiome.values()) {
            Atmosphere atmosphere = Atmospheres.byId(Atmospheres.idOf(a));
            assertNotSame(Atmospheres.NONE, atmosphere);
            for (EndBiome b : EndBiome.values()) {
                if (a != b) assertNotSame(atmosphere, Atmospheres.byId(Atmospheres.idOf(b)));
            }
        }
    }

    @Test
    void unknownIdIsNone() {
        assertSame(Atmospheres.NONE, Atmospheres.byId(99));
        assertSame(Atmospheres.NONE, Atmospheres.byId(-1));
    }
}
