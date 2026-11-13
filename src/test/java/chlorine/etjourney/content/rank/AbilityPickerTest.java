package chlorine.etjourney.content.rank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

/** Picks as many fitting abilities as asked, never the same one twice. */
class AbilityPickerTest {

    private static final List<String> ALL = Arrays.asList("split", "warp", "summon");

    @Test
    void picksDistinctAbilities() {
        Random random = new Random(7);
        for (int i = 0; i < 200; i++) {
            List<String> picked = AbilityPicker.pick(ALL, a -> true, 2, random);
            assertEquals(2, picked.size());
            assertEquals(2, new HashSet<>(picked).size());
        }
    }

    @Test
    void skipsAbilitiesThatDoNotFit() {
        Random random = new Random(7);
        for (int i = 0; i < 200; i++) {
            List<String> picked = AbilityPicker.pick(ALL, a -> !a.equals("warp"), 2, random);
            assertFalse(picked.contains("warp"));
        }
    }

    @Test
    void givesWhatFitsWhenAskedForMore() {
        assertEquals(
            1,
            AbilityPicker.pick(ALL, a -> a.equals("warp"), 3, new Random(1))
                .size());
        assertTrue(
            AbilityPicker.pick(ALL, a -> true, 0, new Random(1))
                .isEmpty());
    }

    @Test
    void everyAbilityGetsPicked() {
        Random random = new Random(3);
        HashSet<String> seen = new HashSet<>();
        for (int i = 0; i < 100; i++) seen.addAll(AbilityPicker.pick(ALL, a -> true, 1, random));
        assertEquals(new HashSet<>(ALL), seen);
    }
}
