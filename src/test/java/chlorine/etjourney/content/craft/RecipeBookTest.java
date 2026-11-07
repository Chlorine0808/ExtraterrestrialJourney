package chlorine.etjourney.content.craft;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

/** A recipe matches only the exact pile of items, in any order, and only where its conditions hold. */
class RecipeBookTest {

    private static final ItemKey GOLD = new ItemKey("etjourney:ingot_ender_gold", 0, "ingotEnderGold");
    private static final ItemKey PLATINUM = new ItemKey("etjourney:ingot_ender_platinum", 0, "ingotEnderPlatinum");
    private static final ItemKey DIRT = new ItemKey("minecraft:dirt", 0);
    private static final ItemOutput OUT = new ItemOutput("minecraft:gold_ingot", 0, 1);
    private static final ConditionContext ANYWHERE = new ConditionContext("Grove", 1, true, 18000L);

    private static List<ItemKey> pile(ItemKey... keys) {
        return Arrays.asList(keys);
    }

    @Test
    void theExactPileMatchesInAnyOrder() {
        Recipe r = Recipe.builder(OUT, 5)
            .input(ItemInput.ore("ingotEnderGold", 2))
            .input(ItemInput.item("etjourney:ingot_ender_platinum", 0, 1))
            .build();
        RecipeBook book = new RecipeBook(Collections.singletonList(r));
        assertSame(r, book.find(null, pile(GOLD, PLATINUM, GOLD), ANYWHERE));
        assertSame(r, book.find(null, pile(PLATINUM, GOLD, GOLD), ANYWHERE));
    }

    @Test
    void tooFewOrTooManyItemsDoNotMatch() {
        Recipe r = Recipe.builder(OUT, 5)
            .input(ItemInput.ore("ingotEnderGold", 2))
            .build();
        RecipeBook book = new RecipeBook(Collections.singletonList(r));
        assertNull(book.find(null, pile(GOLD), ANYWHERE));
        assertNull(book.find(null, pile(GOLD, GOLD, GOLD), ANYWHERE));
        assertNull(book.find(null, pile(GOLD, GOLD, DIRT), ANYWHERE));
        assertNull(book.find(null, pile(), ANYWHERE));
    }

    @Test
    void anItemFillsTheInputItCanServeWhenAnotherCouldTakeIt() {
        // A greedy match would give the gold to the ore input first and leave nothing for the item input.
        Recipe r = Recipe.builder(OUT, 1)
            .input(ItemInput.ore("ingotEnderGold", 1))
            .input(ItemInput.item("etjourney:ingot_ender_gold", 0, 1))
            .build();
        RecipeBook book = new RecipeBook(Collections.singletonList(r));
        assertSame(r, book.find(null, pile(GOLD, GOLD), ANYWHERE));
    }

    @Test
    void theCentreMustMatchItsOwnInput() {
        Recipe r = Recipe.builder(OUT, 200)
            .center(ItemInput.ore("ingotEnderGold", 1))
            .input(ItemInput.ore("ingotEnderPlatinum", 1))
            .build();
        RecipeBook book = new RecipeBook(Collections.singletonList(r));
        assertSame(r, book.find(GOLD, pile(PLATINUM), ANYWHERE));
        assertNull(book.find(PLATINUM, pile(GOLD), ANYWHERE));
        assertNull(book.find(null, pile(GOLD, PLATINUM), ANYWHERE));
    }

    @Test
    void aRecipeWithoutACentreRefusesOne() {
        Recipe r = Recipe.builder(OUT, 1)
            .input(ItemInput.ore("ingotEnderGold", 1))
            .build();
        RecipeBook book = new RecipeBook(Collections.singletonList(r));
        assertNull(book.find(GOLD, pile(GOLD), ANYWHERE));
    }

    @Test
    void conditionsMustAllHold() {
        Recipe r = Recipe.builder(OUT, 1)
            .input(ItemInput.ore("ingotEnderGold", 1))
            .condition(new BiomeCondition("Grove"))
            .condition(new SkyCondition())
            .build();
        RecipeBook book = new RecipeBook(Collections.singletonList(r));
        assertSame(r, book.find(null, pile(GOLD), ANYWHERE));
        assertNull(book.find(null, pile(GOLD), new ConditionContext("Highland", 1, true, 18000L)));
        assertNull(book.find(null, pile(GOLD), new ConditionContext("Grove", 1, false, 18000L)));
    }

    @Test
    void theFirstRecipeThatMatchesWins() {
        Recipe strict = Recipe.builder(OUT, 1)
            .input(ItemInput.ore("ingotEnderGold", 1))
            .condition(new BiomeCondition("Highland"))
            .build();
        Recipe loose = Recipe.builder(new ItemOutput("minecraft:iron_ingot", 0, 1), 1)
            .input(ItemInput.ore("ingotEnderGold", 1))
            .build();
        RecipeBook book = new RecipeBook(Arrays.asList(strict, loose));
        assertSame(strict, book.find(null, pile(GOLD), new ConditionContext("Highland", 1, true, 0L)));
        assertSame(loose, book.find(null, pile(GOLD), ANYWHERE));
    }
}
