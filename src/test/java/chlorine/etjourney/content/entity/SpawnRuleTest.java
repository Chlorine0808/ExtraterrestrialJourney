package chlorine.etjourney.content.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import chlorine.etjourney.world.end.feature.EndBiome;

/** A spawn rule passes a site only when every condition holds; ranges include both ends. */
class SpawnRuleTest {

    private static SpawnRule.Builder grove() {
        return SpawnRule.builder(EnumSet.of(EndBiome.GROVE), false);
    }

    private static SpawnSite site(EndBiome biome, int y, int light, int nearby) {
        return new SpawnSite(true, biome, y, light, nearby);
    }

    @Test
    void defaultsMatchTheSpec() {
        SpawnRule rule = grove().build();
        assertEquals(0, rule.minY);
        assertEquals(255, rule.maxY);
        assertEquals(7, rule.maxLight);
        assertEquals(4, rule.cap);
        assertEquals(32, rule.capRadius);
        assertEquals(10, rule.weight);
        assertEquals(1, rule.groupMin);
        assertEquals(2, rule.groupMax);
        assertTrue(rule.allows(site(EndBiome.GROVE, 64, 0, 0)));
    }

    @Test
    void innerEndIsRefused() {
        assertFalse(
            grove().build()
                .allows(new SpawnSite(false, EndBiome.GROVE, 64, 0, 0)));
    }

    @Test
    void onlyListedBiomesPass() {
        SpawnRule rule = grove().build();
        assertTrue(rule.allows(site(EndBiome.GROVE, 64, 0, 0)));
        assertFalse(rule.allows(site(EndBiome.HIGHLAND, 64, 0, 0)));
    }

    @Test
    void bareLandNeedsTheFlag() {
        assertFalse(
            grove().build()
                .allows(site(null, 64, 0, 0)));
        assertTrue(
            SpawnRule.builder(EnumSet.noneOf(EndBiome.class), true)
                .build()
                .allows(site(null, 64, 0, 0)));
    }

    @Test
    void heightBoundsAreInclusive() {
        SpawnRule rule = grove().height(10, 20)
            .build();
        assertTrue(rule.allows(site(EndBiome.GROVE, 10, 0, 0)));
        assertTrue(rule.allows(site(EndBiome.GROVE, 20, 0, 0)));
        assertFalse(rule.allows(site(EndBiome.GROVE, 9, 0, 0)));
        assertFalse(rule.allows(site(EndBiome.GROVE, 21, 0, 0)));
    }

    @Test
    void lightBoundIsInclusive() {
        SpawnRule rule = grove().maxLight(7)
            .build();
        assertTrue(rule.allows(site(EndBiome.GROVE, 64, 7, 0)));
        assertFalse(rule.allows(site(EndBiome.GROVE, 64, 8, 0)));
    }

    @Test
    void capCountsNeighboursBelowIt() {
        SpawnRule rule = grove().cap(4, 32)
            .build();
        assertTrue(rule.allows(site(EndBiome.GROVE, 64, 0, 3)));
        assertFalse(rule.allows(site(EndBiome.GROVE, 64, 0, 4)));
    }

    @Test
    void badRangesAreRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> grove().height(5, 4)
                .build());
        assertThrows(
            IllegalArgumentException.class,
            () -> grove().weight(0, 1, 1)
                .build());
        assertThrows(
            IllegalArgumentException.class,
            () -> grove().weight(1, 2, 1)
                .build());
        assertThrows(
            IllegalArgumentException.class,
            () -> grove().cap(0, 32)
                .build());
    }
}
