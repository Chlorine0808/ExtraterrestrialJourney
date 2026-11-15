package chlorine.etjourney.world.atmosphere;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.EndBiome;

/** The air mixes the four grid points around the camera, then eases toward that mix over time. */
class AtmosphereBlendTest {

    private static final byte AMBER = Atmospheres.idOf(EndBiome.AMBER);

    /** Bare ground on the west half (points x = 0..2), amber from x = 3 on. */
    private static AtmosphereGrid split() {
        byte[] ids = new byte[AtmosphereGrid.SIZE * AtmosphereGrid.SIZE];
        for (int z = 0; z < AtmosphereGrid.SIZE; z++) {
            for (int x = 3; x < AtmosphereGrid.SIZE; x++) ids[z * AtmosphereGrid.SIZE + x] = AMBER;
        }
        return new AtmosphereGrid(0, 0, ids);
    }

    @Test
    void onAPointIsThatPoint() {
        Atmosphere a = AtmosphereBlend.at(split(), 3 * 32, 32);
        assertEquals(Atmospheres.byId(AMBER).r, a.r, 1e-5);
        assertEquals(Atmospheres.byId(AMBER).density, a.density, 1e-5);
    }

    @Test
    void halfwayMixesEvenly() {
        Atmosphere amber = Atmospheres.byId(AMBER), none = Atmospheres.NONE;
        Atmosphere a = AtmosphereBlend.at(split(), 2.5 * 32, 32);
        assertEquals((amber.g + none.g) / 2, a.g, 1e-5);
        assertEquals((amber.skyMix + none.skyMix) / 2, a.skyMix, 1e-5);
    }

    @Test
    void dominantPicksTheHeavierPoint() {
        assertEquals(AMBER, AtmosphereBlend.dominant(split(), 2.8 * 32, 32));
        assertEquals(0, AtmosphereBlend.dominant(split(), 2.2 * 32, 32));
    }

    @Test
    void easingSnapsFirstThenHalvesTheGapPerHalfLife() {
        AtmosphereBlend.Easing easing = new AtmosphereBlend.Easing();
        Atmosphere none = Atmospheres.NONE, amber = Atmospheres.byId(AMBER);
        assertEquals(none.r, easing.step(none, 0.05).r, 1e-6);
        Atmosphere half = easing.step(amber, AtmosphereBlend.Easing.HALF_LIFE);
        assertEquals((none.r + amber.r) / 2, half.r, 1e-5);
        assertEquals((none.density + amber.density) / 2, half.density, 1e-5);
    }

    @Test
    void resetSnapsAgain() {
        AtmosphereBlend.Easing easing = new AtmosphereBlend.Easing();
        easing.step(Atmospheres.NONE, 0.05);
        easing.reset();
        Atmosphere amber = Atmospheres.byId(AMBER);
        assertEquals(amber.b, easing.step(amber, 0.05).b, 1e-6);
    }
}
