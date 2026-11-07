package chlorine.etjourney.content.craft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Work adds up while the same recipe keeps matching and starts over whenever it changes. */
class CraftProgressTest {

    private static Recipe recipe(int duration) {
        return Recipe.builder(new ItemOutput("minecraft:gold_ingot", 0, 1), duration)
            .input(ItemInput.ore("ingotEnderGold", 1))
            .build();
    }

    @Test
    void finishesWhenTheWorkReachesTheDuration() {
        Recipe r = recipe(3);
        CraftProgress progress = new CraftProgress();
        assertFalse(progress.advance(r, 1));
        assertFalse(progress.advance(r, 1));
        assertTrue(progress.advance(r, 1));
        assertEquals(0, progress.done());
    }

    @Test
    void largeStepsFinishAtOnce() {
        CraftProgress progress = new CraftProgress();
        assertTrue(progress.advance(recipe(5), 10));
    }

    @Test
    void anotherRecipeStartsOver() {
        Recipe a = recipe(3), b = recipe(3);
        CraftProgress progress = new CraftProgress();
        progress.advance(a, 2);
        assertFalse(progress.advance(b, 1));
        assertEquals(1, progress.done());
    }

    @Test
    void noMatchStartsOver() {
        Recipe r = recipe(3);
        CraftProgress progress = new CraftProgress();
        progress.advance(r, 2);
        assertFalse(progress.advance(null, 1));
        assertEquals(0, progress.done());
        assertFalse(progress.advance(r, 2));
    }
}
