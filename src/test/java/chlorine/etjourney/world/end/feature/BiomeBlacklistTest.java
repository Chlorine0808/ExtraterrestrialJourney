package chlorine.etjourney.world.end.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

/** The config names biomes by ID or name and mods by mod ID; a biome matching any of them stays out of balls. */
class BiomeBlacklistTest {

    @Test
    void numbersAreIdsAndTheRestAreNames() {
        BiomeBlacklist list = new BiomeBlacklist(new String[] { "8", "Ice Plains" }, new String[0]);
        assertTrue(list.excludes(8, "Hell", "minecraft"));
        assertTrue(list.excludes(12, "Ice Plains", "minecraft"));
        assertFalse(list.excludes(1, "Plains", "minecraft"));
    }

    @Test
    void namesAndModsIgnoreCaseAndSurroundingSpaces() {
        BiomeBlacklist list = new BiomeBlacklist(new String[] { "  ice plains " }, new String[] { " BiomesOPlenty" });
        assertTrue(list.excludes(12, "Ice Plains", "minecraft"));
        assertTrue(list.excludes(40, "Bamboo Forest", "biomesoplenty"));
    }

    @Test
    void aBiomeOfNoKnownModIsLeftToItsIdAndName() {
        BiomeBlacklist list = new BiomeBlacklist(new String[0], new String[] { "minecraft" });
        assertFalse(list.excludes(99, "Mystery", null));
        assertTrue(list.excludes(1, "Plains", "minecraft"));
    }

    @Test
    void blankEntriesAndNullNamesAreIgnored() {
        BiomeBlacklist list = new BiomeBlacklist(new String[] { "", "  " }, new String[] { "" });
        assertTrue(list.isEmpty());
        assertFalse(list.excludes(0, null, null));
    }

    @Test
    void aNumberTooLongForAnIdIsNoCrash() {
        BiomeBlacklist list = new BiomeBlacklist(new String[] { "99999999999" }, new String[0]);
        assertFalse(list.excludes(1, "Plains", "minecraft"));
        assertEquals(
            Collections.singletonList("biome 99999999999"),
            list.unmatched(Collections.singleton(1), Collections.singleton("Plains"), Collections.emptySet()));
    }

    /** Entries that match nothing are reported, so a typo does not pass unnoticed. */
    @Test
    void entriesMatchingNothingAreListed() {
        BiomeBlacklist list = new BiomeBlacklist(
            new String[] { "8", "Plains", "Nowhere", "300" },
            new String[] { "minecraft", "nomod" });
        assertEquals(
            Arrays.asList("biome Nowhere", "biome 300", "mod nomod"),
            list.unmatched(Arrays.asList(8, 1), Arrays.asList("Hell", "Plains"), Collections.singleton("minecraft")));
    }
}
