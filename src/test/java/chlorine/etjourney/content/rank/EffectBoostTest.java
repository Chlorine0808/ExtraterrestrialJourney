package chlorine.etjourney.content.rank;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/** Only an effect the hit gave or lengthened is strengthened, by the attacker's rank. */
class EffectBoostTest {

    @Test
    void aNewEffectIsLengthenedAndRaised() {
        assertArrayEquals(new int[] { 300, 0 }, EffectBoost.boost(EffectBoost.ABSENT, 0, 200, 0, Rank.RARE));
        assertArrayEquals(new int[] { 400, 2 }, EffectBoost.boost(EffectBoost.ABSENT, 0, 200, 1, Rank.EPIC));
    }

    @Test
    void aRefreshedEffectIsBoostedToo() {
        assertArrayEquals(new int[] { 300, 0 }, EffectBoost.boost(50, 0, 200, 0, Rank.RARE));
        assertArrayEquals(new int[] { 150, 1 }, EffectBoost.boost(400, 0, 100, 1, Rank.RARE));
    }

    @Test
    void anUntouchedEffectIsLeftAlone() {
        assertNull(EffectBoost.boost(200, 0, 199, 0, Rank.EPIC));
        assertNull(EffectBoost.boost(200, 1, 200, 1, Rank.EPIC));
    }

    @Test
    void aNormalAttackerChangesNothing() {
        assertNull(EffectBoost.boost(EffectBoost.ABSENT, 0, 200, 0, Rank.NORMAL));
    }
}
