package chlorine.etjourney.content.rank;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

/** An empty allow list means every monster; the deny list and bosses always win. */
class RankEligibilityTest {

    private static final List<String> NONE = Collections.emptyList();

    @Test
    void anEmptyAllowListTakesEveryMonster() {
        assertTrue(RankEligibility.allows("Enderman", true, false, NONE, NONE));
        assertFalse(RankEligibility.allows("Pig", false, false, NONE, NONE));
    }

    @Test
    void anAllowListTakesOnlyTheNamedMobs() {
        List<String> allow = Arrays.asList("etjourney.spore_zombie", "Pig");
        assertTrue(RankEligibility.allows("etjourney.spore_zombie", true, false, allow, NONE));
        assertTrue(RankEligibility.allows("Pig", false, false, allow, NONE));
        assertFalse(RankEligibility.allows("Enderman", true, false, allow, NONE));
    }

    @Test
    void theDenyListAndBossesWin() {
        List<String> named = Arrays.asList("Enderman", "EnderDragon");
        assertFalse(RankEligibility.allows("Enderman", true, false, named, Arrays.asList("Enderman")));
        assertFalse(RankEligibility.allows("EnderDragon", true, true, named, NONE));
        assertFalse(RankEligibility.allows(null, true, false, NONE, NONE));
    }
}
