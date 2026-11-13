package chlorine.etjourney.core.accessory.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

class DistinctTest {

    /** A worn item: its kind is what makes two of them the same, the tag tells copies apart. */
    private static final class Worn {

        final String kind;
        final String tag;
        final boolean stacks;

        Worn(String kind, String tag, boolean stacks) {
            this.kind = kind;
            this.tag = tag;
            this.stacks = stacks;
        }

        @Override
        public String toString() {
            return kind + "/" + tag;
        }
    }

    private static List<Worn> distinct(Worn... worn) {
        return Distinct.of(Arrays.asList(worn), w -> w.kind, w -> w.stacks);
    }

    @Test
    void twoCopiesCountOnceAndTheFirstWins() {
        Worn first = new Worn("ring", "a", false), second = new Worn("ring", "b", false);
        assertEquals(Collections.singletonList(first), distinct(first, second));
    }

    @Test
    void differentAccessoriesBothCount() {
        Worn ring = new Worn("ring", "a", false), charm = new Worn("charm", "a", false);
        assertEquals(Arrays.asList(ring, charm), distinct(ring, charm));
    }

    @Test
    void anAccessoryThatStacksCountsEveryCopy() {
        Worn first = new Worn("heart", "a", true), second = new Worn("heart", "b", true);
        assertEquals(Arrays.asList(first, second), distinct(first, second));
    }

    @Test
    void keepsTheWornOrder() {
        Worn a = new Worn("a", "1", false), b = new Worn("b", "1", false), a2 = new Worn("a", "2", false);
        assertEquals(Arrays.asList(b, a), distinct(b, a, a2));
    }
}
