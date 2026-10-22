package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class EndBiomeTest {

    private static final long SEED = 7L;

    @Test
    void everyBiomeTakesAFairShare() {
        Map<EndBiome, Integer> counts = new EnumMap<>(EndBiome.class);
        int total = 0;
        for (int i = -60; i < 60; i++) {
            for (int j = -60; j < 60; j++) {
                counts.merge(EndBiome.at(SEED, i * 97.0, j * 89.0), 1, Integer::sum);
                total++;
            }
        }
        assertEquals(EndBiome.values().length, counts.size());
        for (Map.Entry<EndBiome, Integer> e : counts.entrySet())
            assertTrue(e.getValue() > total / 8, e.getKey() + " only " + e.getValue() + " of " + total);
    }

    /** Biomes are drawn on their own map: one zone cell may hold several biomes. */
    @Test
    void bordersAreIndependentOfZones() {
        Map<Zone, Set<EndBiome>> seen = new EnumMap<>(Zone.class);
        for (int i = -40; i < 40; i++) {
            for (int j = -40; j < 40; j++) {
                double x = i * 61.0, z = j * 67.0;
                seen.computeIfAbsent(Zone.at(SEED, x, z), k -> EnumSet.noneOf(EndBiome.class))
                    .add(EndBiome.at(SEED, x, z));
            }
        }
        for (Set<EndBiome> biomes : seen.values()) assertTrue(biomes.size() > 1, "biomes follow zones: " + seen);
    }

    @Test
    void sameSeedSamePlaceSameBiome() {
        for (int i = 0; i < 50; i++)
            assertEquals(EndBiome.at(SEED, i * 131.0, -i * 17.0), EndBiome.at(SEED, i * 131.0, -i * 17.0));
    }
}
