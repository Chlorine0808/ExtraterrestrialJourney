package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.Test;

class EndBiomeTest {

    private static final long SEED = 7L;

    @Test
    void aboutHalfTheContinentsAreBare() {
        Set<Long> ids = new HashSet<>();
        for (int i = -40; i < 40; i++) {
            for (int j = -40; j < 40; j++) {
                Continent.Seed s = Continent.seedAt(SEED, i * 23, j * 29);
                if (s != null) ids.add(ContinentIds.of(SEED, s));
            }
        }
        int bare = 0;
        Set<EndBiome> seen = EnumSet.noneOf(EndBiome.class);
        for (long id : ids) {
            EndBiome b = EndBiome.ofContinent(SEED, id);
            if (b == null) bare++;
            else seen.add(b);
        }
        assertTrue(ids.size() > 100, "only " + ids.size() + " continents");
        assertTrue(bare > ids.size() * 0.35 && bare < ids.size() * 0.65, bare + " of " + ids.size() + " bare");
        assertEquals(EnumSet.allOf(EndBiome.class), seen);
    }

    /** Walking over land, the biome only changes where the land breaks, not in the middle of a continent. */
    @Test
    void biomeChangesOnlyAcrossTheVoid() {
        int landSteps = 0, changes = 0;
        for (int k = 0; k < 8; k++) {
            double angle = k * Math.PI / 4 + 0.3;
            double dx = Math.cos(angle), dz = Math.sin(angle);
            EndBiome prev = null;
            boolean prevLand = false;
            for (int step = 0; step < 1500; step++) {
                double d = 1100 + step * 4.0, x = dx * d, z = dz * d;
                boolean land = Land.height(SEED, x, z, 0, 0) > 0;
                EndBiome b = EndBiome.at(SEED, x, z);
                if (land && prevLand) {
                    landSteps++;
                    if (!Objects.equals(b, prev)) changes++;
                }
                prev = b;
                prevLand = land;
            }
        }
        assertTrue(landSteps > 1000, "only " + landSteps + " steps on land");
        assertTrue(changes <= landSteps / 200, changes + " changes in " + landSteps + " steps on land");
    }

    @Test
    void sameSeedSamePlaceSameBiome() {
        for (int i = 0; i < 50; i++) assertEquals(
            EndBiome.at(SEED, 1100 + i * 131.0, -i * 17.0),
            EndBiome.at(SEED, 1100 + i * 131.0, -i * 17.0));
    }
}
